package er.sproutcore;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOAssociation;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WODynamicElement;
import com.webobjects.appserver.WOElement;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;

import er.extensions.appserver.ERXResponse;
import er.extensions.foundation.ERXThreadStorage;

public class SCJavaScript extends WODynamicElement {
    
    protected Logger log = Logger.getLogger(getClass());

    WOAssociation _name;
    WOAssociation _framework;

    public SCJavaScript(String arg0, NSDictionary arg1, WOElement arg2) {
        super(arg0, arg1, arg2);
        _name = (WOAssociation) arg1.objectForKey("name");
        _framework = (WOAssociation) arg1.objectForKey("framework");
    }

    @Override
    public void appendToResponse(WOResponse response, WOContext context) {
        ERXResponse scriptResponse = ERXResponse.pushPartial(SCPageTemplate.CLIENT_JS);
        String name = (String) _name.valueInComponent(context.component());
        String framework;
        if(_framework == null) {
            framework = "SproutCore";
            appendScript(scriptResponse, context, "SproutCore/prototype/prototype.js");
        } else {
            framework = (String) _framework.valueInComponent(context.component());
        }
        NSArray<String> scripts = SCUtilities.require(framework, name);
        log.info("adding: " +scripts);
        for (String script : scripts) {
            appendScript(scriptResponse, context, script);
        }
        ERXResponse.popPartial();

    }

    public static void appendScript(WOResponse response, WOContext context, String name) {
        NSMutableArray<String> scripts = (NSMutableArray<String>) ERXThreadStorage.valueForKey("SCRequire.Scripts");
        if(scripts == null) {
            scripts = new NSMutableArray<String>();
            ERXThreadStorage.takeValueForKey(scripts, "SCRequire.Scripts");
        }
        if(!scripts.contains(name)) {
            String url = context.urlWithRequestHandlerKey(SproutCore.SC_KEY,name, null);
            response.appendContentString("<script");
            response._appendTagAttributeAndValue("type", "text/javascript", false);
            response._appendTagAttributeAndValue("src", url, false);
            response.appendContentString("></script>\n");
            scripts.addObject(name);
        }
    }
    
}