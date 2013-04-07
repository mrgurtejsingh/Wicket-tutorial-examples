/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wicketTutorial.websocket;

import java.util.Map;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.behavior.IBehaviorListener;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.crypt.Base64;
import org.apache.wicket.util.lang.Generics;
import org.apache.wicket.util.template.PackageTextTemplate;


/**
 * 
 * The main class to add a webscoket callback method to a component. This class adds itself to
 * the application-scoped WebsocketBehaviorsManager and generates the JavaScript needed to open 
 * a websocket from client side.
 * 
 * @see WsBehaviorAndWebRequest
 * @see WebsocketBehaviorsManager
 */
public class WebsocketBehavior extends Behavior{
	
	private Component component;
	private String pageId;
	private String sessionId;
	private String behaviorId;	
	
	public static final MetaDataKey<WebsocketBehaviorsManager> WEBSOCKET_BEHAVIOR_MAP_KEY = 
			new MetaDataKey<WebsocketBehaviorsManager>(){};
	public static final String WEBSOCKET_CREATOR_URL = "/websocketCreator";
	private String baseUrl;
 	
	@Override
	public void onConfigure(Component component) {
		super.onConfigure(component);
		
		if(Session.get().isTemporary())
			Session.get().bind();
		
		this.component = component;
		this.pageId = component.getPage().getId();		
		this.sessionId = Session.get().getId();
		WebsocketBehaviorsManager behaviorsManager = Application.get().getMetaData(WEBSOCKET_BEHAVIOR_MAP_KEY);
		
		CharSequence behaviorUrl = component.urlFor(this, IBehaviorListener.INTERFACE, null);
		this.behaviorId = Base64.encodeBase64URLSafeString(behaviorUrl.toString().getBytes());
		
		Request request = RequestCycle.get().getRequest();
		WsBehaviorAndWebRequest behavAndReq = new WsBehaviorAndWebRequest((WebRequest)request, this);
		
		behaviorsManager.putBehavior(behavAndReq, sessionId, behaviorId);				
		
		baseUrl = extractBaseUrl(request).toString();
	}
	
	protected void onMessage(WebsocketRequestTarget target, 
		String message, boolean last){						
	}
	
	@Override
	public void renderHead(Component component, IHeaderResponse response){	
		baseUrl = baseUrl.replaceAll("http", "ws");
		baseUrl = baseUrl.replaceAll("https", "wss");
		
		final ResourceReference ajaxReference = Application.get().getJavaScriptLibrarySettings().getWicketAjaxReference();
		final PackageResourceReference openWebsocket = new PackageResourceReference(WebsocketBehavior.class, "res/openWebsocket.js");
		
		String socketUrl =  baseUrl + WEBSOCKET_CREATOR_URL + "?sessionId=" + sessionId +
				"&behaviorId=" + behaviorId;
		Map<String, Object> variables = Generics.newHashMap();
		
		variables.put("socketUrl", socketUrl);
		variables.put("componentId", component.getMarkupId());
		
		PackageTextTemplate webSocketSetupTemplate =
				new PackageTextTemplate(WebsocketBehavior.class, "res/openWebsocket.template.js");
				
		response.render(JavaScriptHeaderItem.forReference(ajaxReference));
		response.render(JavaScriptHeaderItem.forReference(openWebsocket));
		
		response.render(OnLoadHeaderItem.forScript(webSocketSetupTemplate.asString(variables)));
	}


	protected CharSequence extractBaseUrl(Request request) {
		RequestCycle requestCycle = RequestCycle.get();
		Url relative = Url.parse(requestCycle.getRequest().getContextPath());
		String full = requestCycle.getUrlRenderer().renderFullUrl(relative);
		
		return full;
	}

	@Override
	public boolean getStatelessHint(Component component){
		return false;
	}

	@Override
	public boolean isTemporary(Component component) {
		return false;
	}

	protected Component getComponent() {
		return component;
	}


	protected String getPageId() {
		return pageId;
	}
}
