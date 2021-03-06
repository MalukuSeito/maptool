/*
 * This software copyright by various authors including the RPTools.net
 * development team, and licensed under the LGPL Version 3 or, at your option,
 * any later version.
 *
 * Portions of this software were originally covered under the Apache Software
 * License, Version 1.1 or Version 2.0.
 *
 * See the file LICENSE elsewhere in this distribution for license details.
 */

package net.rptools.maptool.client.ui.chat;

import java.util.ArrayList;
import java.util.List;

public class ChatProcessor {

	private List<ChatTranslationRuleGroup> translationRuleGroupList = new ArrayList<ChatTranslationRuleGroup>();

	public String process(String incoming) {
		if (incoming == null) {
			return null;
		}

		for (ChatTranslationRuleGroup ruleGroup : translationRuleGroupList) {
			if (!ruleGroup.isEnabled()) {
				continue;
			}
			incoming = ruleGroup.translate(incoming);
		}
		return incoming;
	}

	public void install(ChatTranslationRuleGroup ruleGroup) {
		translationRuleGroupList.add(ruleGroup);
	}
}
