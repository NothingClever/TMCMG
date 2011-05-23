package togos.noise2.function;

import togos.noise2.data.DataDa;
import togos.noise2.data.DataDaDaDa;

public class CosDaDaDa_Da extends OneArgDaDaDa_Da
{
	public CosDaDaDa_Da( FunctionDaDaDa_Da arg ) {
		super(arg);
	}
	
	public String getMacroName() {
	    return "cos";
    }

	public DataDa apply( DataDaDaDa in ) {
		int len = in.getLength();
		double[] dat = arg.apply(in).x;
		double[] out = new double[len];
		for (int i = 0; i < len; i++) {
			out[i] = Math.cos(dat[i]);
		}
		return new DataDa( out );
	}
}
