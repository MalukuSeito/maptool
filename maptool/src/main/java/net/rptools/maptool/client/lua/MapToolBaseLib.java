/**
 * 
 */
package net.rptools.maptool.client.lua;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.client.MapToolLUAParser;
import net.rptools.maptool.client.MapToolLineParser;
import net.rptools.maptool.client.MapToolMacroContext;
import net.rptools.maptool.client.MapToolVariableResolver;
import net.rptools.maptool.client.ui.macrobuttons.buttons.MacroButtonPrefs;
import net.rptools.maptool.language.I18N;
import net.rptools.maptool.model.MacroButtonProperties;
import net.rptools.maptool.model.Token;
import net.rptools.parser.ParserException;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.BaseLib;

/**
 * @author Maluku
 *
 */
public class MapToolBaseLib extends BaseLib {
	Globals globals;
	MapToolVariableResolver resolver;
	MapToolMacroContext context;

	public MapToolBaseLib(MapToolVariableResolver res, Token tokenInContext, MapToolMacroContext context) {
		resolver = res;
		this.context = context;
	}

	@Override
	public LuaValue call(LuaValue modname, LuaValue env) {
		super.call(modname, env);
		globals = env.checkglobals();
		globals.STDIN = System.in;
		return env;
	}

	@Override
	public InputStream findResource(String lua) {
		try {
			return MapToolLUAParser.compile(find(lua), lua, globals);
		} catch (IOException e) {
			throw new LuaError(e);
		}
	}

	public InputStream find(String lua) {
		String macro = lua;
		if (macro.endsWith(".lua")) {
			macro = macro.substring(0, macro.length() - 4);
		}
		try {
			String[] macroParts = macro.split("@", 2);
			String macroLocation;

			String macroName = macroParts[0];
			if (macroParts.length == 1) {
				macroLocation = null;
			} else {
				macroLocation = macroParts[1];
			}
			// For convenience to macro authors, no error on a blank macro name
			if (macroName.equalsIgnoreCase(""))
				throw new LuaError("Can't import empty string");

			// IF the macro is a  @this, then we get the location of the current macro and use that.
			if (macroLocation != null && macroLocation.equalsIgnoreCase("this")) {
				macroLocation = MapTool.getParser().getMacroSource();
				if (macroLocation.equals(MapToolLineParser.CHAT_INPUT) || macroLocation.toLowerCase().startsWith("token:")) {
					macroLocation = "TOKEN";
				}
			}
			Token tokenInContext = resolver.getTokenInContext();
			if (macroLocation == null || macroLocation.length() == 0 || macroLocation.equals(MapToolLineParser.CHAT_INPUT)) {
				// Unqualified names are not allowed.
				throw new ParserException(I18N.getText("lineParser.invalidMacroLoc", macroName));
			} else if (macroLocation.equalsIgnoreCase("TOKEN")) {
				checkContext(MapTool.getPlayer().isGM(), macro);
				// Search token for the macro
				if (tokenInContext != null) {
					MacroButtonProperties buttonProps = tokenInContext.getMacro(macroName, false);
					if (buttonProps == null) {
						throw new ParserException(I18N.getText("lineParser.atTokenNotFound", macroName));
					}
					return new ByteArrayInputStream(buttonProps.getCommand().getBytes());
				}
				throw new LuaError("Can't import " + macro + ": No Token impersonated");
			} else if (macroLocation.equalsIgnoreCase("CAMPAIGN")) {
				MacroButtonProperties mbp = null;
				for (MacroButtonProperties m : MapTool.getCampaign().getMacroButtonPropertiesArray()) {
					if (m.getLabel().equals(macroName)) {
						mbp = m;
						break;
					}
				}
				if (mbp == null) {
					throw new ParserException(I18N.getText("lineParser.unknownCampaignMacro", macroName));
				}
				checkContext(!mbp.getAllowPlayerEdits(), macro);
				return new ByteArrayInputStream(mbp.getCommand().getBytes());
			} else if (macroLocation.equalsIgnoreCase("GLOBAL")) {
				checkContext(MapTool.getPlayer().isGM(), macro);
				MacroButtonProperties mbp = null;
				for (MacroButtonProperties m : MacroButtonPrefs.getButtonProperties()) {
					if (m.getLabel().equals(macroName)) {
						mbp = m;
						break;
					}
				}
				if (mbp == null) {
					throw new ParserException(I18N.getText("lineParser.unknownGlobalMacro", macroName));
				}
				return new ByteArrayInputStream(mbp.getCommand().getBytes());
			} else { // Search for a token called macroLocation (must start with "Lib:")
				String macroBody = MapTool.getParser().getTokenLibMacro(macroName, macroLocation);
				Token token = MapTool.getParser().getTokenMacroLib(macroLocation);

				if (macroBody == null || token == null) {
					throw new ParserException(I18N.getText("lineParser.unknownMacro", macroName));
				}
				checkContext(MapTool.getParser().isSecure(macroName, token), macro);
				return new ByteArrayInputStream(macroBody.getBytes());
			}
		} catch (ParserException e) {
			throw new LuaError(e);
		} catch (Exception e) {
			throw e;
		}
	}

	private void checkContext(boolean secure, String macro) {
		if (context != null && context.isTrusted() && !secure) {
			throw new LuaError("A trusted macro can not import " + macro + ", an untrusted macro, use macro.run() instead");
		}
	}
}
