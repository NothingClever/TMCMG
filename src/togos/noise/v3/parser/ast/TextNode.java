package togos.noise.v3.parser.ast;

import togos.noise.v3.parser.Token;

public class TextNode extends ASTNode
{
	public enum Type {
		BAREWORD,
		SINGLE_QUOTED,
		DOUBLE_QUOTED;
		
		public static Type fromTokenType( Token.Type tt ) throws RuntimeException {
			switch( tt ) {
			case BAREWORD: case SYMBOL: return BAREWORD;
			case DOUBLE_QUOTED_STRING: return DOUBLE_QUOTED;
			case SINGLE_QUOTED_STRING: return SINGLE_QUOTED;
			default: throw new RuntimeException("Don't know how to convert token type '"+tt+"' to text node type");
			}
		}
	};
	
	public final Type type;
	public final String text;
	
	public TextNode( Type type, Token t ) {
		super(t);
		this.type = type;
		this.text = t.text;
	}
	
	public String toString() {
		return this.text;
	}
	public String toAtomicString() { return toString(); }
}
