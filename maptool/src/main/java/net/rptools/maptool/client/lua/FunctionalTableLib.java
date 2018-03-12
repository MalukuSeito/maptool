package net.rptools.maptool.client.lua;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.rptools.maptool.client.functions.JSONMacroFunctions;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.VarArgFunction;

public class FunctionalTableLib extends TableLib {

	public FunctionalTableLib() {
	}

	public LuaValue call(LuaValue modname, LuaValue env) {
		LuaValue result = super.call(modname, env);
		LuaTable table = env.get("table").checktable();
		table.set("map", new map());
		table.set("reduce", new reduce());
		table.set("length", new length());
		table.set("indent", new indent());
		table.set("contains", new contains());
		table.set("indexOf", new indexof());
		table.set("containsKey", new contains());
		table.set("containsValue", new contains());
		table.set("count", new count());
		table.set("keys", new keys());
		table.set("values", new values());
		table.set("union", new union());
		table.set("difference", new difference());
		table.set("intersection", new intersection());
		table.set("merge", new merge());
		table.set("ordered", new ordered());
		table.set("shuffle", new shuffle());
		table.set("equals", new equals());
		return result;
	}

	static class TableLibFunction extends LibFunction {
		public LuaValue call() {
			return argerror(1, "table expected, got no value");
		}
	}

	static class map extends TableLibFunction {
		public LuaValue call(LuaValue list) {
			return argerror(2, "function expected, got no value");
		}

		public LuaValue call(LuaValue list, LuaValue func) {
			LuaTable result = new LuaTable();
			func.checkfunction();
			for (LuaValue val : LuaConverters.arrayIterate(list.checktable())) {
				result.insert(0, func.call(val));
			}
			return result;
		}
	}
	
	static class equals extends TableLibFunction {
		public LuaValue call(LuaValue list) {
			return valueOf(list.isnil());
		}
		
		public LuaValue call(LuaValue list, LuaValue list2) {
			return compare(list, list2, new HashSet<LuaValue>());
		}
		
		private LuaValue compare(LuaValue list, LuaValue list2, Set<LuaValue> seen) {
		
			if (list.istable() && list2.istable()) {
				LuaTable table1 = list.checktable();
				LuaTable table2 = list2.checktable();
				if (table1.eq_b(table2)) {
					return TRUE;
				} else if (seen.contains(table1)) {
					return FALSE;
				}
				Set<LuaValue> s = new HashSet<LuaValue>(seen);
				s.add(table1);
				ArrayList<LuaValue> keys = new ArrayList<>();
				for (Varargs v = table1.next(NIL); !v.arg1().isnil(); v = table1.next(v.arg1())) {
					LuaValue v2 = table2.get(v.arg1());
					if (!TRUE.equals(compare(v.arg(2), v2, s))) return FALSE;
					keys.add(v.arg1());
				}
				for (Varargs v = table2.next(NIL); !v.arg1().isnil(); v = table2.next(v.arg1())) {
					if (!keys.contains(v.arg1())) return FALSE;
				}
				return TRUE;
			}
			return list.eq(list2);
		}
	}

	static class reduce extends TableLibFunction {
		public LuaValue call(LuaValue list) {
			return argerror(2, "function expected, got no value");
		}

		public LuaValue call(LuaValue list, LuaValue func) {
			LuaValue result = NIL;
			func.checkfunction();
			for (LuaValue val : LuaConverters.arrayIterate(list.checktable())) {
				result = func.call(result, val);
			}
			return result;
		}
	}

	static class length extends TableLibFunction {
		public LuaValue call(LuaValue list) {
			LuaTable l = list.checktable();
			int count = 0;
			Varargs next = l.next(LuaValue.NIL);
			while (!next.isnil(1)) {
				count++;
				next = l.next(next.arg1());
			}
			return LuaValue.valueOf(count);
		}
	}

	static class keys extends TableLibFunction {
		public LuaValue call(LuaValue list) {
			LuaTable l = list.checktable();
			LuaTable result = new LuaTable();
			Varargs next = l.next(LuaValue.NIL);
			while (!next.isnil(1)) {
				result.insert(0, next.arg1());
				next = l.next(next.arg1());
			}
			return result;
		}
	}

	static class values extends TableLibFunction {
		public LuaValue call(LuaValue list) {
			LuaTable l = list.checktable();
			LuaTable result = new LuaTable();
			Varargs next = l.next(LuaValue.NIL);
			while (!next.isnil(1)) {
				result.insert(0, next.arg(2));
				next = l.next(next.arg1());
			}
			return result;
		}
	}
	
	static class ordered extends TableLibFunction {
		public LuaValue call(LuaValue table) {
			LuaTable result = new InsertionOrderLuaTable();
			if (table.istable()) {
				Varargs next = table.next(LuaValue.NIL);
				while (!next.isnil(1)) {
					result.rawset(next.arg1(), next.arg(2));
					next = table.next(next.arg1());
				}
			}
			return result;
		}
	}

	static class indent extends TableLibFunction {
		@Override
		public LuaValue call(LuaValue a) {
			return call(a, valueOf(4));
		}

		public LuaValue call(LuaValue list, LuaValue indent) {
			Object obj;
			if (list.isstring()) {
				obj = JSONMacroFunctions.asJSON(list.tostring());
			} else {
				obj = LuaConverters.toJson(list);
			}
			if (obj instanceof JSONObject) {
				return valueOf(((JSONObject) obj).toString(indent.checkint()));
			} else if (obj instanceof JSONArray) {
				return valueOf(((JSONArray) obj).toString(indent.checkint()));
			}
			return list;
		}
	}

	static class shuffle extends TableLibFunction {
		public LuaValue call(LuaValue list) {
			if (list.istable()) {
				LuaTable table = list.checktable();
				ArrayList<LuaValue> l = new ArrayList<LuaValue>(table.length());
				for (LuaValue v: LuaConverters.arrayIterate(table)) {
					l.add(v);
				}
				Collections.shuffle(l);
				int i = 0;
				for (LuaValue v: l) {
					table.rawset(++i, v);
				}
			}
			return list;
		}
	}
	
	static class contains extends TableLibFunction {
		public LuaValue call(LuaValue list) {
			return argerror(2, "value expected, got no value");
		}

		public LuaValue call(LuaValue list, LuaValue val) {
			LuaTable l = list.checktable();
			Varargs next = l.inext(LuaValue.ZERO);
			while (!next.isnil(1)) {
				if (next.arg(2).eq_b(val)) {
					return TRUE;
				}
				next = l.next(next.arg1());
			}
			return FALSE;
		}
	}
	
	static class indexof extends TableLibFunction {
		public LuaValue call(LuaValue list) {
			return argerror(2, "value expected, got no value");
		}

		public LuaValue call(LuaValue list, LuaValue val) {
			LuaTable l = list.checktable();
			Varargs next = l.inext(LuaValue.ZERO);
			int c = 0;
			while (!next.isnil(1)) {
				c++;
				if (next.arg(2).eq_b(val)) {
					return valueOf(c);
				}
				next = l.next(next.arg1());
			}
			return ZERO;
		}
	}
	
	static class count extends TableLibFunction {
		public LuaValue call(LuaValue list) {
			return argerror(2, "value expected, got no value");
		}

		public LuaValue call(LuaValue list, LuaValue val) {
			LuaTable l = list.checktable();
			Varargs next = l.next(LuaValue.NIL);
			int c = 0;
			while (!next.isnil(1)) {
				if (next.arg(2).eq_b(val)) {
					c++;
				}
				next = l.next(next.arg1());
			}
			return valueOf(c);
		}
	}

	static class containsKey extends TableLibFunction {
		public LuaValue call(LuaValue list) {
			return argerror(2, "value expected, got no value");
		}

		public LuaValue call(LuaValue list, LuaValue val) {
			LuaTable l = list.checktable();
			Varargs next = l.next(LuaValue.NIL);
			while (!next.isnil(1)) {
				if (next.arg(1).eq_b(val)) {
					return TRUE;
				}
				next = l.next(next.arg1());
			}
			return FALSE;
		}
	}

	static class containsValue extends TableLibFunction {
		public LuaValue call(LuaValue list) {
			return argerror(2, "value expected, got no value");
		}

		public LuaValue call(LuaValue list, LuaValue val) {
			LuaTable l = list.checktable();
			Varargs next = l.next(LuaValue.NIL);
			while (!next.isnil(1)) {
				if (next.arg(2).eq_b(val)) {
					return TRUE;
				}
				next = l.next(next.arg1());
			}
			return FALSE;
		}
	}

	static class union extends VarArgFunction {
		@Override
		public Varargs invoke(Varargs args) {

			LuaTable t = args.arg1().checktable();
			boolean map = false;
			for (Varargs n = t.next(NIL); !n.arg1().isnil(); n = t.next(n.arg1())) {
				if (!n.arg1().isint()) {
					map = true;
					break;
				}
			}
			if (map) {
				LuaTable r = new LuaTable();
				for (int i = 1, nb = args.narg(); i <= nb; i++) {
					t = args.arg(i).checktable();
					for (Varargs n = t.next(NIL); !n.arg1().isnil(); n = t.next(n.arg1())) {
						r.rawset(n.arg1(), n.arg(2));
					}
				}
				return r;
			} else {
				Set<LuaValue> result = new HashSet<LuaValue>();
				for (int i = 1, nb = args.narg(); i <= nb; i++) {
					t = args.arg(i).checktable();
					for (Varargs n = t.inext(ZERO); !n.arg1().isnil(); n = t.inext(n.arg1())) {
						result.add(n.arg(2));
					}
				}
				LuaTable r = new LuaTable();
				for (LuaValue v : result) {
					r.insert(0, v);
				}
				return r;
			}
		}
	}

	static class difference extends VarArgFunction {
		@Override
		public Varargs invoke(Varargs args) {

			LuaTable t = args.arg1().checktable();
			boolean map = false;
			for (Varargs n = t.next(NIL); !n.arg1().isnil(); n = t.next(n.arg1())) {
				if (!n.arg1().isint()) {
					map = true;
					break;
				}
			}
			if (map) {
				LuaTable r = new LuaTable();
				for (Varargs n = t.next(NIL); !n.arg1().isnil(); n = t.next(n.arg1())) {
					r.rawset(n.arg1(), n.arg(2));
				}
				for (int i = 2, nb = args.narg(); i <= nb; i++) {
					t = args.arg(i).checktable();
					for (Varargs n = t.next(NIL); !n.arg1().isnil(); n = t.next(n.arg1())) {
						r.rawset(n.arg1(), NIL);
					}
				}
				return r;
			} else {
				List<LuaValue> result = new ArrayList<LuaValue>();
				for (Varargs n = t.inext(ZERO); !n.arg1().isnil(); n = t.inext(n.arg1())) {
					result.add(n.arg(2));
				}
				for (int i = 2, nb = args.narg(); i <= nb; i++) {
					t = args.arg(i).checktable();
					for (Varargs n = t.inext(ZERO); !n.arg1().isnil(); n = t.inext(n.arg1())) {
						result.remove(n.arg(2));
					}
				}
				LuaTable r = new LuaTable();
				for (LuaValue v : result) {
					r.insert(0, v);
				}
				return r;
			}
		}
	}

	static class merge extends VarArgFunction {
		@Override
		public Varargs invoke(Varargs args) {

			LuaTable t = args.arg1().checktable();
			boolean map = false;
			for (Varargs n = t.next(NIL); !n.arg1().isnil(); n = t.next(n.arg1())) {
				if (!n.arg1().isint()) {
					map = true;
					break;
				}
			}
			if (map) {
				LuaTable r = new LuaTable();
				for (int i = 1, nb = args.narg(); i <= nb; i++) {
					t = args.arg(i).checktable();
					for (Varargs n = t.next(NIL); !n.arg1().isnil(); n = t.next(n.arg1())) {
						r.rawset(n.arg1(), n.arg(2));
					}
				}
				return r;
			} else {
				LuaTable r = new LuaTable();
				for (int i = 1, nb = args.narg(); i <= nb; i++) {
					t = args.arg(i).checktable();
					for (Varargs n = t.inext(ZERO); !n.arg1().isnil(); n = t.inext(n.arg1())) {
						r.insert(0, n.arg(2));
					}
				}
				return r;
			}
		}
	}

	static class intersection extends VarArgFunction {
		@Override
		public Varargs invoke(Varargs args) {

			LuaTable t = args.arg1().checktable();
			boolean map = false;
			for (Varargs n = t.next(NIL); !n.arg1().isnil(); n = t.next(n.arg1())) {
				if (!n.arg1().isint()) {
					map = true;
					break;
				}
			}
			if (map) {
				LuaTable r = new LuaTable();
				for (Varargs n = t.next(NIL); !n.arg1().isnil(); n = t.next(n.arg1())) {
					r.rawset(n.arg1(), n.arg(2));
				}
				for (int i = 2, nb = args.narg(); i <= nb; i++) {
					t = args.arg(i).checktable();
					for (Varargs n = r.next(NIL); !n.arg1().isnil(); n = r.next(n.arg1())) {
						if (t.get(n.arg1()).isnil()) {
							r.rawset(n.arg1(), NIL);
						}
					}
				}
				return r;
			} else {
				List<LuaValue> result = new ArrayList<LuaValue>();
				for (Varargs n = t.inext(ZERO); !n.arg1().isnil(); n = t.inext(n.arg1())) {
					result.add(n.arg(2));
				}
				for (int i = 2, nb = args.narg(); i <= nb; i++) {
					Set<LuaValue> result2 = new HashSet<LuaValue>();
					t = args.arg(i).checktable();
					for (Varargs n = t.inext(ZERO); !n.arg1().isnil(); n = t.inext(n.arg1())) {
						result2.add(n.arg(2));
					}
					result.retainAll(result2);
				}
				LuaTable r = new LuaTable();
				for (LuaValue v : result) {
					r.insert(0, v);
				}
				return r;
			}
		}
	}
}
