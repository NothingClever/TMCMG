package togos.noise2.vm.stkernel;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import togos.noise2.lang.CompileError;
import togos.noise2.lang.SourceLocation;

public class Compiler
{
	public static class DumbSourceLocation implements SourceLocation {
		final String filename;
		final int lineNumber;
		public DumbSourceLocation( String filename, int lineNumber ) {
			this.filename = filename;
			this.lineNumber = lineNumber;
		}
		public String getSourceFilename() {  return filename;   }
		public int getSourceLineNumber() {   return lineNumber; }
		public int getSourceColumnNumber() { return 0;          }
	}
	
	protected double[] getDoubleVector( String name, Map vars, String filename, int lineNumber ) throws CompileError {
		Object o = vars.get(name);
		if( o instanceof double[] ) return (double[])o;
		if( o == null ) {
			throw new CompileError("Undefined variable '"+name+"'", new DumbSourceLocation(filename, lineNumber));
		}
		throw new CompileError("Used '"+name+"' in double[] context; it's "+o.getClass(), new DumbSourceLocation(filename, lineNumber));
	}
	
	public STVectorKernel compile(BufferedReader r, String filename, int maxVectorSize) throws IOException, CompileError {
		String line;
		int lineNumber = 0;
		
		HashMap vars = new HashMap();
		ArrayList ops = new ArrayList();
		
		while( (line = r.readLine()) != null ) {
			++lineNumber;
			int cidx = line.indexOf('#');
			if( cidx == 0 ) continue;
			if( cidx > 0 ) {
				line = line.substring(0, cidx);
			}
			line = line.trim();
			if( line.length() == 0 ) continue;
			String[] parts = line.split(" ");
			if( parts.length == 0 ) continue;
			
			if( "vector".equals(parts[0]) ) {
				if( parts.length != 3 ) {
					throw new CompileError("'vector' statement requires 2 arguments", new DumbSourceLocation(filename,lineNumber));
				}
				if( vars.containsKey(parts[2]) ) {
					throw new CompileError("Redefinition of '"+parts[2]+"'", new DumbSourceLocation(filename,lineNumber));
				}
				if( "double".equals(parts[1])) {
					vars.put(parts[2], new double[maxVectorSize]);
					continue;
				} else if( "boolean".equals(parts[1]) ) {
					vars.put(parts[2], new boolean[maxVectorSize]);
					continue;
				}
				throw new CompileError("Invalid vector type '"+parts[1]+"'", new DumbSourceLocation(filename,lineNumber));
			}
			
			if( parts.length == 5 && "=".equals(parts[1]) ) {
				if( "+".equals(parts[3]) ) {
					ops.add( new AddOp(
						getDoubleVector(parts[0], vars, filename, lineNumber),
						getDoubleVector(parts[2], vars, filename, lineNumber),
						getDoubleVector(parts[4], vars, filename, lineNumber)
					));
					continue;
				}
				if( "-".equals(parts[3]) ) {
					ops.add( new SubtractOp(
						getDoubleVector(parts[0], vars, filename, lineNumber),
						getDoubleVector(parts[2], vars, filename, lineNumber),
						getDoubleVector(parts[4], vars, filename, lineNumber)
					));
					continue;
				}
				if( "*".equals(parts[3]) ) {
					ops.add( new MultiplyOp(
						getDoubleVector(parts[0], vars, filename, lineNumber),
						getDoubleVector(parts[2], vars, filename, lineNumber),
						getDoubleVector(parts[4], vars, filename, lineNumber)
					));
					continue;
				}
				if( "/".equals(parts[3]) ) {
					ops.add( new DivideOp(
						getDoubleVector(parts[0], vars, filename, lineNumber),
						getDoubleVector(parts[2], vars, filename, lineNumber),
						getDoubleVector(parts[4], vars, filename, lineNumber)
					));
					continue;
				}
			}
			
			throw new CompileError("Invalid line '"+line+"'", new DumbSourceLocation(filename,lineNumber));
		}
		
		return new STVectorKernel( vars, ops, maxVectorSize );
	}
}