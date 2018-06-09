/**
 * 
 */
package net.rptools.maptool.client.lua;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JseMathLib;

/**
 * @author Maluku
 *
 */

public class MathLib extends JseMathLib {
	@Override
	public LuaValue call(LuaValue modname, LuaValue env) {
		LuaValue math = super.call(modname, env);
		math.set("hypot", new hypot());
		math.set("log10", new log10());
		return math;
	}

	static final class hypot extends BinaryOp {
		protected double call(double d, double o) {
			return Math.hypot(d, o);
		}
	}
	
	static final class log10 extends UnaryOp { 
		protected double call(double d) { 
			return Math.log10(d); 
		} 
	}
}
