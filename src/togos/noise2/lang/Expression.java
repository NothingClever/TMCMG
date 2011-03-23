package togos.noise2.lang;

import togos.noise2.rewrite.ExpressionRewriter;

public interface Expression
{
	/**
	 * Should call v.rewrite() on all sub-expressions, using the
	 * return value to replace the sub-expressions in what is
	 * otherwise a copy of this object.
	 */
	public Object rewriteSubExpressions(ExpressionRewriter v);
	
	/**
	 * Return a list of all sub-expressions
	 * that will be called with the *same inputs* as this expression. 
	 */
	public abstract Expression[] directSubExpressions();
	
	/**
	 * A string that is
	 * - valid TNL and 
	 * - is semantically equivalent to this expression.
	 *   - parts of the expression that make no difference samantically,
	 *     such as cache(), can and should be left out. 
	 */
	public String toTnl();
}
