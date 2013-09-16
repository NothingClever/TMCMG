package togos.noise.v3.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import togos.lang.BaseSourceLocation;
import togos.lang.CompileError;
import togos.lang.RuntimeError;
import togos.noise.MathUtil;
import togos.noise.function.D5_2Perlin;
import togos.noise.function.PngNoise;
import togos.noise.function.SimplexNoise;
import togos.noise.v3.BindingError;
import togos.noise.v3.program.compiler.ExpressionVectorProgramCompiler;
import togos.noise.v3.program.runtime.Binding;
import togos.noise.v3.program.runtime.BoundArgumentList;
import togos.noise.v3.program.runtime.BoundArgumentList.BoundArgument;
import togos.noise.v3.program.runtime.Context;
import togos.noise.v3.program.runtime.Function;
import togos.noise.v3.vector.function.LFunctionDaDaDa_Da;
import togos.noise.v3.vector.vm.Operators;
import togos.noise.v3.vector.vm.Operators.AbstractOperator;
import togos.noise.v3.vector.vm.Operators.OperatorDa_Da;
import togos.noise.v3.vector.vm.Program;
import togos.noise.v3.vector.vm.Program.Instance;
import togos.noise.v3.vector.vm.Program.Instruction;
import togos.noise.v3.vector.vm.Program.Operator;
import togos.noise.v3.vector.vm.Program.RegisterBankID;
import togos.noise.v3.vector.vm.Program.RegisterBankID.BVar;
import togos.noise.v3.vector.vm.Program.RegisterBankID.DVar;
import togos.noise.v3.vector.vm.Program.RegisterID;
import togos.noise.v3.vector.vm.ProgramBuilder;

public class MathFunctions
{
	static final BaseSourceLocation BUILTIN_LOC = new BaseSourceLocation( MathFunctions.class.getName()+".java", 0, 0);
	public static final Context CONTEXT = new Context();
	
	static abstract class FixedArgumentBuiltinFunction<R> extends BuiltinFunction<R> {
		protected final Class<? extends R> returnType;
		protected final Class<?>[] argumentTypes;
		protected final Object[] argumentDefaults;
		
		public FixedArgumentBuiltinFunction(
			Class<? extends R> returnType,
			Class<?>[] argumentTypes,
			Object[] argumentDefaults
		) {
			assert argumentTypes != null;
			assert argumentDefaults != null;
			assert argumentTypes.length == argumentDefaults.length;
			
			this.returnType = returnType;
			this.argumentTypes = argumentTypes;
			this.argumentDefaults = argumentDefaults;
		}
		
		public abstract String getName();
		abstract R apply( Object[] arguments );
		
		public Class<? extends R> getReturnType() { return returnType; }
		
		protected abstract RegisterID<?> toVectorProgram( final Binding<?>[] argumentBindings, ExpressionVectorProgramCompiler compiler ) throws CompileError;
				
		public Binding<R> apply( final BoundArgumentList input ) throws CompileError {
			for( BoundArgument<?> arg : input.arguments ) {
				if( !arg.name.isEmpty() ) {
					throw new CompileError("+ takes no named arguments, but was given '"+arg.name+"'", arg.value.sLoc);
				}
			}
			final Binding<?>[] argumentBindings = new Binding[argumentTypes.length];
			for( int i=0; i<argumentTypes.length; ++i ) {
				if( input.arguments.size() <= i ) {
					if( argumentDefaults[i] == null ) {
						throw new CompileError("Argument "+(i+1)+" required but not given for "+getName(), input.callLocation );
					} else {
						argumentBindings[i] = Binding.forValue( argumentDefaults[i], BUILTIN_LOC );
					}
				} else {
					argumentBindings[i] = input.arguments.get(i).value;
					if( argumentBindings[i].getValueType() != null ) {
						if( !argumentTypes[i].isAssignableFrom(argumentBindings[i].getValueType()) ) {
							throw new BindingError( argumentTypes[i]+" required but argument would return "+argumentBindings[i].getValueType(), argumentBindings[i] );
						}
					}
				}
			}
			return Binding.memoize(new Binding<R>( input.callLocation ) {
				@Override public boolean isConstant() throws CompileError {
					for( int i=0; i<argumentBindings.length; ++i ) {
						if( !argumentBindings[i].isConstant() ) return false;
					}
					return true; 
				}
				
				@Override public R getValue() throws Exception {
					Object[] arguments = new Object[argumentBindings.length];
					for( int i=0; i<arguments.length; ++i ) {
						arguments[i] = argumentBindings[i].getValue();
						if( arguments[i] == null ) {
							throw new RuntimeError("Argument evaluated to null", argumentBindings[i].sLoc);
						}
						if( !argumentTypes[i].isAssignableFrom(arguments[i].getClass()) ) {
							throw new RuntimeError("'"+getName()+"' expected a "+argumentTypes[i]+" but got a "+arguments[i].getClass(), argumentBindings[i].sLoc);
						}
					}
					return apply( arguments );
				}
				
				@Override public Class<? extends R> getValueType() {
					return returnType;
				}
				
				@Override public Collection<Binding<?>> getDirectDependencies() {
					return Arrays.asList(argumentBindings);
				}
				
				public String getCalculationId() throws CompileError {
					return getName() + "(" + input.toSource() + ")";
				}
				
				public RegisterID<?> toVectorProgram( ExpressionVectorProgramCompiler compiler ) throws CompileError {
					return FixedArgumentBuiltinFunction.this.toVectorProgram( argumentBindings, compiler );
				}
			});
		}
	}
	
	static abstract class BooleanInputFunction<R> extends FixedArgumentBuiltinFunction<R> {
		abstract R apply( boolean a, boolean b );
		
		protected static final Class<?>[] ARG_TYPES = { Boolean.class, Boolean.class };
		protected static final Object[] ARG_DEFAULTS = { null, null };

		public BooleanInputFunction( Class<? extends R> returnType ) {
			super(returnType, ARG_TYPES, ARG_DEFAULTS);
		}
		
		protected R apply( Object[] args ) {
			return apply( ((Boolean)args[0]).booleanValue(), ((Boolean)args[1]).booleanValue() );
		}
	}
	
	// TODO: Make sure getOperator always returns
	// the same instance for identical operators
	// so that uses can be cached!
	
	static abstract class FunctionBB_B extends BooleanInputFunction<Boolean> {
		public FunctionBB_B() { super(Boolean.class); }

		protected abstract Operator<RegisterBankID.BVar, RegisterBankID.BVar, RegisterBankID.BVar, RegisterBankID.None> getOperator();

		protected RegisterID<RegisterBankID.BVar> toVectorProgram(Binding<?>[] argumentBindings, ExpressionVectorProgramCompiler compiler) throws CompileError {
			return compiler.pb.bb_b( getOperator(), compiler.compile(argumentBindings[0], RegisterBankID.BVar.INSTANCE), compiler.compile(argumentBindings[1], RegisterBankID.BVar.INSTANCE));
		};
	}
	
	static abstract class BinaryNumberInputFunction<R> extends FixedArgumentBuiltinFunction<R> {
		abstract R apply( double a, double b );
		
		protected static final Class<?>[] ARG_TYPES = { Number.class, Number.class };
		protected static final Object[] ARG_DEFAULTS = { null, null };

		public BinaryNumberInputFunction( Class<? extends R> returnType ) {
			super(returnType, ARG_TYPES, ARG_DEFAULTS);
		}
		
		protected R apply( Object[] args ) {
			return apply( ((Number)args[0]).doubleValue(), ((Number)args[1]).doubleValue() );
		}
	}
	
	static abstract class UnaryNumberInputFunction<R> extends FixedArgumentBuiltinFunction<R> {
		abstract R apply( double a );
		
		protected static final Class<?>[] ARG_TYPES = { Number.class };
		protected static final Object[] ARG_DEFAULTS = { null };

		public UnaryNumberInputFunction( Class<? extends R> returnType ) {
			super(returnType, ARG_TYPES, ARG_DEFAULTS);
		}
		
		protected R apply( Object[] args ) {
			return apply( ((Number)args[0]).doubleValue() );
		}
	}

	
	static abstract class FunctionII_I extends BinaryNumberInputFunction<Integer> {
		public FunctionII_I() { super(Integer.class); }
		
		protected abstract Operator<RegisterBankID.IVar, RegisterBankID.IVar, RegisterBankID.IVar, RegisterBankID.None> getOperator();
		
		protected Program.RegisterID<RegisterBankID.IVar> toVectorProgram( Binding<?>[] argumentBindings, ExpressionVectorProgramCompiler compiler) throws CompileError {
			return compiler.pb.ii_i( getOperator(), compiler.compile(argumentBindings[0], RegisterBankID.IVar.INSTANCE), compiler.compile(argumentBindings[1], RegisterBankID.IVar.INSTANCE));		
		};
	}

	static abstract class FunctionDD_B extends BinaryNumberInputFunction<Boolean> {
		public FunctionDD_B() { super(Boolean.class); }
		
		protected abstract Operator<RegisterBankID.BVar, RegisterBankID.DVar, RegisterBankID.DVar, RegisterBankID.None> getOperator();
		
		protected Program.RegisterID<RegisterBankID.BVar> toVectorProgram( Binding<?>[] argumentBindings, ExpressionVectorProgramCompiler compiler) throws CompileError {
			return compiler.pb.dd_b( getOperator(), compiler.compile(argumentBindings[0], RegisterBankID.DVar.INSTANCE), compiler.compile(argumentBindings[1], RegisterBankID.DVar.INSTANCE));		
		};
	}
	
	static abstract class FunctionD_D extends UnaryNumberInputFunction<Double> {
		public FunctionD_D() { super(Double.class); }
		
		protected abstract OperatorDa_Da getOperator();
		
		protected Program.RegisterID<RegisterBankID.DVar> toVectorProgram( Binding<?>[] argumentBindings, ExpressionVectorProgramCompiler compiler) throws CompileError {
			return compiler.pb.d_d( getOperator(), compiler.compile(argumentBindings[0], RegisterBankID.DVar.INSTANCE) );		
		};
	}
	
	static abstract class FunctionDD_D extends BinaryNumberInputFunction<Double> {
		public FunctionDD_D() { super(Double.class); }
		
		protected abstract Operator<RegisterBankID.DVar, RegisterBankID.DVar, RegisterBankID.DVar, RegisterBankID.None> getOperator();
		
		protected Program.RegisterID<RegisterBankID.DVar> toVectorProgram( Binding<?>[] argumentBindings, ExpressionVectorProgramCompiler compiler) throws CompileError {
			return compiler.pb.dd_d( getOperator(), compiler.compile(argumentBindings[0], RegisterBankID.DVar.INSTANCE), compiler.compile(argumentBindings[1], RegisterBankID.DVar.INSTANCE));		
		};
	}
	
	static abstract class FunctionDDD_D extends FixedArgumentBuiltinFunction<Number> {
		abstract double apply( double a, double b, double c );
		
		protected static final Class<?>[] ARG_TYPES = { Number.class, Number.class, Number.class };
		protected static final Object[] ARG_DEFAULTS = { null, null, null };

		public FunctionDDD_D() {
			super(Number.class, ARG_TYPES, ARG_DEFAULTS);
		}
		
		protected Double apply( Object[] args ) {
			return apply( ((Number)args[0]).doubleValue(), ((Number)args[1]).doubleValue(), ((Number)args[2]).doubleValue() );
		}
		
		protected abstract Operator<RegisterBankID.DVar, RegisterBankID.DVar, RegisterBankID.DVar, RegisterBankID.DVar> getOperator();
		
		protected RegisterID<RegisterBankID.DVar> toVectorProgram(Binding<?>[] argumentBindings, ExpressionVectorProgramCompiler compiler) throws CompileError {
			return compiler.pb.ddd_d( getOperator(),
				compiler.compile(argumentBindings[0], RegisterBankID.DVar.INSTANCE),
				compiler.compile(argumentBindings[1], RegisterBankID.DVar.INSTANCE),
				compiler.compile(argumentBindings[2], RegisterBankID.DVar.INSTANCE)
			);
		};
	}
	
	static abstract class NoiseFunction extends FunctionDDD_D {
		protected abstract LFunctionDaDaDa_Da getLFunction();
		
		protected Operator<DVar, DVar, DVar, DVar> getOperator() {
			return new AbstractOperator<DVar, DVar, DVar, DVar>(getName()) {
				@Override public void apply(Instance pi, Instruction<DVar, DVar, DVar, DVar> inst, int vectorSize) {
					double[] x = pi.doubleVectors[inst.v1.number];
					double[] y = pi.doubleVectors[inst.v2.number];
					double[] z = pi.doubleVectors[inst.v3.number];
					double[] dest = pi.doubleVectors[inst.dest.number];
					getLFunction().apply( vectorSize, x, y, z, dest  );
				}
			};
		}
	}
	
	public static class ConstantBindingFunction<V> implements Function<V> {
		final Binding<? extends V> v;
		
		public ConstantBindingFunction( Binding<? extends V> v ) {
			this.v = v;
		}
		
		@Override public Binding<? extends V> apply( BoundArgumentList input ) {
			return v;
		}
		
		public String getCalculationId() throws CompileError {
			return v.getCalculationId();
		}
	}
	
	protected static <V> Binding<? extends V> builtinBinding( V v ) {
		return Binding.forValue( v, BUILTIN_LOC );
	}
	
	static {
		//// Logic
		
		FunctionBB_B logicalOr = new FunctionBB_B() {
			@Override public String getName() { return "or"; }
			@Override protected Boolean apply( boolean a, boolean b ) { return a || b; }
			@Override protected Operators.OperatorBaBa_Ba getOperator() { return Operators.LOGICAL_OR; }
		};
		FunctionBB_B logicalXor = new FunctionBB_B() {
			@Override public String getName() { return "xor"; }
			@Override protected Boolean apply( boolean a, boolean b ) { return (a && !b) || (b && !a); }
			@Override protected Operators.OperatorBaBa_Ba getOperator() { return Operators.LOGICAL_XOR; }
		};
		FunctionBB_B logicalAnd = new FunctionBB_B() {
			@Override public String getName() { return "and"; }
			@Override Boolean apply( boolean a, boolean b ) { return a && b; }
			@Override protected Operators.OperatorBaBa_Ba getOperator() { return Operators.LOGICAL_AND; }
		};
		
		//// Single-argument functions
		
		CONTEXT.put("sin", builtinBinding(new FunctionD_D() {
			@Override public String getName() { return "sin"; }
			@Override public Double apply( double a ) { return Math.sin(a); }
			@Override protected OperatorDa_Da getOperator() { return Operators.SIN; }
		}));
		CONTEXT.put("asin", builtinBinding(new FunctionD_D() {
			@Override public String getName() { return "asin"; }
			@Override public Double apply( double a ) { return Math.asin(a); }
			@Override protected OperatorDa_Da getOperator() { return Operators.ASIN; }
		}));
		CONTEXT.put("cos", builtinBinding(new FunctionD_D() {
			@Override public String getName() { return "cos"; }
			@Override public Double apply( double a ) { return Math.cos(a); }
			@Override protected OperatorDa_Da getOperator() { return Operators.COS; }
		}));
		CONTEXT.put("acos", builtinBinding(new FunctionD_D() {
			@Override public String getName() { return "acos"; }
			@Override public Double apply( double a ) { return Math.acos(a); }
			@Override protected OperatorDa_Da getOperator() { return Operators.ACOS; }
		}));
		CONTEXT.put("tan", builtinBinding(new FunctionD_D() {
			@Override public String getName() { return "tan"; }
			@Override public Double apply( double a ) { return Math.tan(a); }
			@Override protected OperatorDa_Da getOperator() { return Operators.TAN; }
		}));
		CONTEXT.put("atan", builtinBinding(new FunctionD_D() {
			@Override public String getName() { return "atan"; }
			@Override public Double apply( double a ) { return Math.atan(a); }
			@Override protected OperatorDa_Da getOperator() { return Operators.ATAN; }
		}));
		/** Used commonly enough that I think it makes sense to have its own
		 * function, even though it's equivalent to a ** 0.5 */
		CONTEXT.put("sqrt", builtinBinding(new FunctionD_D() {
			@Override public String getName() { return "sqrt"; }
			@Override public Double apply( double a ) { return Math.sqrt(a); }
			@Override protected OperatorDa_Da getOperator() { return Operators.SQRT; }
		}));

		//// Noise
		
		CONTEXT.put("perlin", builtinBinding(new NoiseFunction() {
			@Override public String getName() { return "perlin"; }
			
			@Override double apply(double a, double b, double c) {
				return D5_2Perlin.instance.get(a, b, c);
			}
			
			@Override protected LFunctionDaDaDa_Da getLFunction() {
				return D5_2Perlin.instance;
			}
		}) );
		
		CONTEXT.put("simplex", builtinBinding(new NoiseFunction() {
			@Override public String getName() { return "simplex"; }
			
			ThreadLocal<SimplexNoise> simplex = new ThreadLocal<SimplexNoise>() {
				@Override public SimplexNoise initialValue() {
					return new SimplexNoise();
				}
			};
			
			@Override double apply(double a, double b, double c) {
				return simplex.get().apply((float)a, (float)b, (float)c);
			}
			
			@Override protected LFunctionDaDaDa_Da getLFunction() {
				return simplex.get();
			}
		}) );
		
		CONTEXT.put("png", builtinBinding(new NoiseFunction() {
			@Override public String getName() { return "png"; }
			
			ThreadLocal<PngNoise> image = new ThreadLocal<PngNoise>() {
				@Override public PngNoise initialValue() {
					return new PngNoise();
				}
			};
			
			@Override double apply(double a, double b, double c) {
				return image.get().apply((float)a, (float)b, (float)c);
			}
			
			@Override protected LFunctionDaDaDa_Da getLFunction() {
				return image.get();
			}
		}) );
		CONTEXT.put("if", builtinBinding(new BuiltinFunction<Object>() {
			@Override public String getName() { return "if"; }
			
			@Override public Binding<Object> apply(final BoundArgumentList input) throws CompileError {
				if( input.hasNamedArguments() ) throw new CompileError("'if' does not take named arguments", input.argListLocation);
				if( input.arguments.size() < 3 || input.arguments.size() % 2 == 0 ) {
					throw new CompileError("'if' requires an odd number of arguments >= 3", input.argListLocation);
				}
				return Binding.memoize(new Binding<Object>( input.callLocation ) {
					@Override public boolean isConstant() throws CompileError {
						try {
							int i = 0;
							while( i <= input.arguments.size()-2 ) {
								Binding<? extends Boolean> condition = Binding.cast(input.arguments.get(i).value, Boolean.class);
								if( condition.isConstant() ) {
									if( condition.getValue().booleanValue() ) {
										Binding<?> value = input.arguments.get(i+1).value;
										return value.isConstant();
									} else {
										// Value will never be used, so skip to the next!
										i += 2;
									}
								} else {
									return false;
								}
							}
							return input.arguments.get(i).value.isConstant();
						} catch( CompileError e ) {
							throw e;
						} catch( Exception e ) {
							throw new RuntimeException( e );
						}
					}
					
					@Override public Object getValue() throws Exception {
						int i = 0;
						while( i <= input.arguments.size()-2 ) {
							Binding<? extends Boolean> condition = Binding.cast(input.arguments.get(i).value, Boolean.class);
							if( condition.getValue().booleanValue() ) {
								return input.arguments.get(i+1).value.getValue();
							}
							i += 2;
						}
						return input.arguments.get(i).value.getValue();
					}
					
					@Override public Class<? extends Object> getValueType() throws CompileError {
		 			   return null;
					}
					
					@Override public Collection<Binding<?>> getDirectDependencies() {
						return input.getArgumentBindings();
					}
					
					@Override public String getCalculationId() throws CompileError {
						return "if(" + input.toSource() + ")";
					}
					
					@Override
					public RegisterID<?> toVectorProgram( ExpressionVectorProgramCompiler compiler) throws CompileError {
						final ProgramBuilder pb = compiler.pb;
						
						// TODO: Factor this crap out into another function
						ArrayList<Binding<?>> simplified = new ArrayList<Binding<?>>();
						int i=0;
						simplify: {
							while( i <= input.arguments.size()-2 ) {
								Binding<? extends Boolean> condition = Binding.cast(input.arguments.get(i).value, Boolean.class);
								Binding<?> value = input.arguments.get(i+1).value;
								if( condition.isConstant() ) {
									boolean alwaysSatisfied;
									try {
										alwaysSatisfied = condition.getValue().booleanValue();
									} catch( CompileError e ) {
										throw e;
									} catch( Exception e ) {
										throw new RuntimeException(e);
									}
									if( alwaysSatisfied ) {
										// Then quit here and don't bother with the rest of the list!
										simplified.add(value);
										break simplify;
									} else {
										// Then skip this pair!
									}
								} else {
									simplified.add(condition);
									simplified.add(value);
								}
								i += 2;
							}
							assert i == input.arguments.size()-1;
							simplified.add(input.arguments.get(i).value);
						}
						
						
						
						i = simplified.size()-1;
						RegisterID<?> onFalseRegister = compiler.compile( simplified.get(i) );
						i -= 2;
						while( i >= 0 ) {
							assert i % 2 == 0;
							Binding<? extends Boolean> condition = Binding.cast(simplified.get(i), Boolean.class);
							
							RegisterID<BVar> conditionRegister = (RegisterID<BVar>)pb.translate(condition.toVectorProgram(compiler), Boolean.class, condition.sLoc);
							RegisterID<?> onTrueRegister = compiler.compile(simplified.get(i+1));
							onFalseRegister = pb.select(conditionRegister, onFalseRegister, onTrueRegister, condition.sLoc);
							i -= 2;
						}
						return onFalseRegister;
					}
				});
			}
		}));
		CONTEXT.put("&&",  builtinBinding(logicalAnd));
		CONTEXT.put("^^",  builtinBinding(logicalXor));
		CONTEXT.put("||",  builtinBinding(logicalOr));
		CONTEXT.put("and", builtinBinding(logicalAnd));
		CONTEXT.put("xor", builtinBinding(logicalXor));
		CONTEXT.put("or",  builtinBinding(logicalOr));
		
		CONTEXT.put("<<", builtinBinding(new FunctionII_I() {
			@Override public String getName() { return "<<"; }
			@Override protected Integer apply( double a, double b ) { return (int)a << (int)b; }
			@Override protected Operators.OperatorIaIa_Ia getOperator() { return Operators.BITSHIFT_RIGHT; }
		}));
		CONTEXT.put(">>", builtinBinding(new FunctionII_I() {
			@Override public String getName() { return "<<"; }
			@Override protected Integer apply( double a, double b ) { return (int)a >> (int)b; }
			@Override protected Operators.OperatorIaIa_Ia getOperator() { return Operators.BITSHIFT_LEFT; }
		}));
		CONTEXT.put("|", builtinBinding(new FunctionII_I() {
			@Override public String getName() { return "|"; }
			@Override protected Integer apply( double a, double b ) { return (int)a | (int)b; }
			@Override protected Operators.OperatorIaIa_Ia getOperator() { return Operators.BITWISE_OR; }
		}));
		CONTEXT.put("&", builtinBinding(new FunctionII_I() {
			@Override public String getName() { return "&"; }
			@Override protected Integer apply( double a, double b ) { return (int)a & (int)b; }
			@Override protected Operators.OperatorIaIa_Ia getOperator() { return Operators.BITWISE_AND; }
		}));
		CONTEXT.put("^", builtinBinding(new FunctionII_I() {
			@Override public String getName() { return "^"; }
			@Override protected Integer apply( double a, double b ) { return (int)a ^ (int)b; }
			@Override protected Operators.OperatorIaIa_Ia getOperator() { return Operators.BITWISE_XOR; }
		}));
		
		CONTEXT.put(">", builtinBinding(new FunctionDD_B() {
			@Override public String getName() { return ">"; }
			@Override Boolean apply( double a, double b ) { return a > b; }
			@Override protected Operators.OperatorDaDa_Ba getOperator() { return Operators.COMPARE_GREATER; }
		}));
		CONTEXT.put(">=", builtinBinding(new FunctionDD_B() {
			@Override public String getName() { return ">="; }
			@Override Boolean apply( double a, double b ) { return a >= b; }
			@Override protected Operators.OperatorDaDa_Ba getOperator() { return Operators.COMPARE_GREATER_OR_EQUAL; }
		}));
		CONTEXT.put("==", builtinBinding(new FunctionDD_B() {
			@Override public String getName() { return "=="; }
			@Override Boolean apply( double a, double b ) { return a == b; }
			@Override protected Operators.OperatorDaDa_Ba getOperator() { return Operators.COMPARE_EQUAL; }
		}));
		CONTEXT.put("!=", builtinBinding(new FunctionDD_B() {
			@Override public String getName() { return "!="; }
			@Override Boolean apply( double a, double b ) { return a != b; }
			@Override protected Operators.OperatorDaDa_Ba getOperator() { return Operators.COMPARE_NOT_EQUAL; }
		}));
		CONTEXT.put("<=", builtinBinding(new FunctionDD_B() {
			@Override public String getName() { return "<="; }
			@Override Boolean apply( double a, double b ) { return a <= b; }
			@Override protected Operators.OperatorDaDa_Ba getOperator() { return Operators.COMPARE_LESSER_OR_EQUAL; }
		}));
		CONTEXT.put("<", builtinBinding(new FunctionDD_B() {
			@Override public String getName() { return "<"; }
			@Override Boolean apply( double a, double b ) { return a < b; }
			@Override protected Operators.OperatorDaDa_Ba getOperator() { return Operators.COMPARE_LESSER; }
		}));
		
		CONTEXT.put("+", builtinBinding(new FunctionDD_D() {
			@Override public String getName() { return "+"; }
			@Override Double apply( double a, double b ) { return a + b; }
			@Override protected Operators.OperatorDaDa_Da getOperator() { return Operators.ADD; }
		}));
		CONTEXT.put("-", builtinBinding(new FunctionDD_D() {
			@Override public String getName() { return "-"; }
			@Override Double apply( double a, double b ) { return a - b; }
			@Override protected Operators.OperatorDaDa_Da getOperator() { return Operators.SUBTRACT; }
		}));
		CONTEXT.put("*", builtinBinding(new FunctionDD_D() {
			@Override public String getName() { return "*"; }
			@Override Double apply( double a, double b ) { return a * b; }
			@Override protected Operators.OperatorDaDa_Da getOperator() { return Operators.MULTIPLY; }
		}));
		CONTEXT.put("/", builtinBinding(new FunctionDD_D() {
			@Override public String getName() { return "/"; }
			@Override Double apply( double a, double b ) { return a / b; }
			@Override protected Operators.OperatorDaDa_Da getOperator() { return Operators.DIVIDE; }
		}));
		CONTEXT.put("**", builtinBinding(new FunctionDD_D() {
			@Override public String getName() { return "*"; }
			@Override Double apply( double a, double b ) { return Math.pow(a, b); }
			@Override protected Operators.OperatorDaDa_Da getOperator() { return Operators.EXPONENTIATE; }
		}));
		CONTEXT.put("%", builtinBinding(new FunctionDD_D() {
			@Override public String getName() { return "%"; }
			@Override Double apply( double a, double b ) { return MathUtil.safeFlooredDivisionModulus(a, b); }
			@Override protected Operators.OperatorDaDa_Da getOperator() { return Operators.FLOORED_DIVISION_MODULUS; }
		}));
		
		CONTEXT.put("true", builtinBinding(Boolean.TRUE));
		CONTEXT.put("false", builtinBinding(Boolean.FALSE));
	}
}
