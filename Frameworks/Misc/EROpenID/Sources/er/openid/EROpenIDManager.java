package er.openid;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Logger;
import org.openid4java.association.AssociationException;
import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageException;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.Parameter;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.openid4java.message.sreg.SRegMessage;
import org.openid4java.message.sreg.SRegRequest;
import org.openid4java.util.HttpClientFactory;
import org.openid4java.util.ProxyProperties;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORedirect;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOSession;
import com.webobjects.foundation.NSDictionary;

import er.extensions.appserver.ERXApplication;
import er.extensions.appserver.ERXRequest;
import er.extensions.foundation.ERXProperties;

/**
 * EROpenIDManager is the primary interface to managing an OpenID connection.
 *
 * @property er.openid.proxyHostName the host name to use as a proxy (if necessary)
 * @property er.openid.proxyPort the port to use as a proxy (if necessary)
 * @property er.openid.formRedirectionPageName the name of the form redirection page to go to 
 *                                             (defaults to EROFormRedirectionPage, should implement IEROFormRedirectionPage)
 * @property er.openid.requireSecureReturnURL whether to require secure return URL; default is true
 * @property er.openid.disableRealmVerifier whether to disable realm verifier. see OpenID 2.0 specification.
 * 
 * @author mschrag
 */
public class EROpenIDManager {
  public static final Logger log = Logger.getLogger(EROpenIDManager.class);

  private static final String DISCOVERY_INFO_KEY = "openIDDiscoveryInfo";
  private static EROpenIDManager _openIDManager;
  private ConsumerManager _manager;
  private EROpenIDManager.Delegate _delegate;

  /**
   * EROpenIDManager delegate
   */
  public static interface Delegate {
    /**
     * Returns OpenID message extensions for the fetch request. These message extensions will be sent to the OP
     * as requests for additional information.
     *
     * @param userSuppliedString the string the user supplied
     * @param request the WORequest
     * @param context the WOContext
     * @return a FetchRequest
     * @throws MessageException
     */
    public List<MessageExtension> createFetchMessageExtensions(String userSuppliedString, WORequest request, WOContext context) throws MessageException;
  
    /**
     * Returns an OpenID fetch request.
     *  
     * @param userSuppliedString the string the user supplied
     * @param request the WORequest
     * @param context the WOContext
     * @return a FetchRequest
     * @throws MessageException 
     * @deprecated Replaced by createFetchMessageExtensions
     */
    @Deprecated
    public MessageExtension createFetchRequest(String userSuppliedString, WORequest request, WOContext context) throws MessageException;

    /**
     * Called after a response is received from the OpenID server.
     *  
     * @param verification the original verification result
     * @param eroResponse the EROResponse wrapper
     * @param request the WORequest
     * @param context the WOContext
     */
    public void responseReceived(VerificationResult verification, EROResponse eroResponse, WORequest request, WOContext context);

    /**
     * Returns the URL that the OpenID provider should come back to after authentication.
     * 
     * @param request the WORequest
     * @param context the WOContext
     * @return the URL that the OpenID provider should come back to after authentication
     */
    public String returnToUrl(WORequest request, WOContext context);

    /**
     * Gives the delegate an opportunity to rewrite the receiving URL from the return
     * request after authentication.  This may be necessary if you are using rewrite
     * rules on your webserver, and the returnToUrl does not match the URL that 
     * WebObjects may see in the WORequest.
     * 
     * @param request the WORequest
     * @param context the WOContext
     * @return the rewritten URL
     */
    public String rewriteReceivingUrl(WORequest request, WOContext context);
  }

  /**
   * The default delegate implementation.
   */
  public static class DefaultDelegate implements EROpenIDManager.Delegate {

    @SuppressWarnings("unused")
    public List<MessageExtension> createFetchMessageExtensions(String userSuppliedString, WORequest request, WOContext context) throws MessageException {
      MessageExtension fetchRequest = this.createFetchRequest(userSuppliedString, request, context);
      ArrayList<MessageExtension> exts = new ArrayList<MessageExtension>();
      if (fetchRequest != null)
          exts.add(fetchRequest);
      return exts;
    }

    @SuppressWarnings("unused")
    @Deprecated
    public MessageExtension createFetchRequest(String userSuppliedString, WORequest request, WOContext context) throws MessageException {
      return null;
    }

    public void responseReceived(VerificationResult verification, EROResponse eroResponse, WORequest request, WOContext context) {
      // DO NOTHING
    }

    public String returnToUrl(WORequest request, WOContext context) {
      String returnToUrl;
      boolean requireSecureReturnURL = ERXProperties.booleanForKeyWithDefault("er.openid.requireSecureReturnURL", true);
      if (ERXApplication.isWO54()) {
        try {
          if (requireSecureReturnURL) {
            Method directActionURLForActionNamedMethod = context.getClass().getMethod("directActionURLForActionNamed", new Class[] { String.class, NSDictionary.class, boolean.class, boolean.class });
            returnToUrl = (String) directActionURLForActionNamedMethod.invoke(context, new Object[] { "ERODirectAction/openIDResponse", null, Boolean.TRUE, Boolean.TRUE });
          }
          else {
            returnToUrl = context.directActionURLForActionNamed("ERODirectAction/openIDResponse", new NSDictionary());
          }
        }
        catch (Exception e) {
          throw new RuntimeException("directActionURLForActionNamed failed.", e);
        }
      }
      else {
        context._generateCompleteURLs();
        try {
          if (requireSecureReturnURL) {
            Method _directActionURLMethod = context.getClass().getMethod("_directActionURL", new Class[] { String.class, NSDictionary.class, boolean.class });
            returnToUrl = (String) _directActionURLMethod.invoke(context, new Object[] { "ERODirectAction/openIDResponse", null, Boolean.TRUE });
          }
          else {
            returnToUrl = context.directActionURLForActionNamed("ERODirectAction/openIDResponse", new NSDictionary());
          }
        }
        catch (Exception e) {
          throw new RuntimeException("_directActionURL failed.", e);
        }
        finally {
          context._generateRelativeURLs();
        }
      }
      EROpenIDManager.log.debug("Return to URL: " + returnToUrl);
      return returnToUrl;
    }

    public String rewriteReceivingUrl(WORequest request, WOContext context) {
      StringBuffer receivingUrlBuffer = new StringBuffer();
      int serverPort = 0;
      String serverPortStr = request._serverPort();
      if (serverPortStr != null) {
        serverPort = Integer.parseInt(serverPortStr);
      }
      request._completeURLPrefix(receivingUrlBuffer, ERXRequest.isRequestSecure(request), serverPort);
      receivingUrlBuffer.append(request.uri());
      return receivingUrlBuffer.toString();
    }
  }

  /**
   * A simple delegate implementation that requests the user's email address. This is for example purposes only and
   * this code is not suitable for production use. To utilize this particular delegate, the client should ask the
   * EROResponse for a list of MessageExtensions. Then the client should iterate through those and retrieve the
   * specific extension parameter using methods appropriate to the type of the MessageExtension. For instance, if
   * the MessageExtension is an instance of SRegResponse, then the email parameter could be retrieved by casting the
   * MessageExtension to an SRegResponse and then using getAttributeValue("email") to retrieve the email.
   */
  public static class EmailDelegate extends EROpenIDManager.DefaultDelegate {
    @Override
    public List<MessageExtension> createFetchMessageExtensions(String userSuppliedString, WORequest request, WOContext context) throws MessageException {
      ArrayList<MessageExtension> exts = new ArrayList<MessageExtension>();
      FetchRequest fetchRequest = FetchRequest.createFetchRequest();
      fetchRequest.addAttribute("email-axschema", "http://axschema.org/contact/email", true);
      fetchRequest.addAttribute("email-openid", "http://schema.openid.net/contact/email", true);
      exts.add(fetchRequest);
      SRegRequest sregRequest = SRegRequest.createFetchRequest();
      sregRequest.addAttribute("email",true);
      exts.add(sregRequest);
      return exts;
    }
  }

  /**
   * Returns the singleton EROpenIDManager instance.
   * 
   * @return the singleton EROpenIDManager instance
   */
  public static synchronized EROpenIDManager manager() {
    if (_openIDManager == null) {
      try {
        _openIDManager = new EROpenIDManager();
        _openIDManager.setDelegate(new DefaultDelegate());
      }
      catch (ConsumerException e) {
        throw new RuntimeException("Failed to create EROpenIDManager.", e);
      }
    }
    return _openIDManager;
  }

  /**
   * Constructs a new EROpenIDManager.
   * 
   * @throws ConsumerException
   */
  protected EROpenIDManager() throws ConsumerException {
    _manager = new ConsumerManager();
    boolean disableRealmVerifier = ERXProperties.booleanForKeyWithDefault("er.openid.disableRealmVerifier", false);
    if (disableRealmVerifier) {
      _manager.getRealmVerifier().setEnforceRpId(false);
      EROpenIDManager.log.info("Disabling realm verifier.");
    }
  }

  /**
   * Set the delegate for this manager if you want to specify a custom
   * FetchRequest.
   * 
   * @param delegate the new delegate
   */
  public void setDelegate(EROpenIDManager.Delegate delegate) {
    _delegate = delegate;
  }

  /**
   * Returns whether or not the given string looks like an OpenID auth string.
   * 
   * @param userSuppliedString the string from the user
   * @return whether or not the given string looks like an OpenID auth string
   */
  public boolean isOpenIDAuth(String userSuppliedString) {
    return userSuppliedString != null && userSuppliedString.toLowerCase().startsWith("http://");
  }

  /**
   * Initiates the authentication request.
   * 
   * @param userSuppliedString the string supplied by the user
   * @param realm explicit realm to use for the authRequest. Allows nulls.
   * @param request the WORequest
   * @param context the WOContext
   * @return the redirection action results
   * @throws MessageException
   * @throws DiscoveryException
   * @throws ConsumerException
   */
  public WOActionResults authRequest(String userSuppliedString, String realm, WORequest request, WOContext context) throws MessageException, DiscoveryException, ConsumerException {
    WOSession session = context.session();

    String proxyHostName = ERXProperties.stringForKey("er.openid.proxyHostName");
    if (proxyHostName != null) {
      int proxyPort = ERXProperties.intForKey("er.openid.proxyPort");
      // --- Forward proxy setup (only if needed) ---
      ProxyProperties proxyProps = new ProxyProperties();
      proxyProps.setProxyHostName(proxyHostName);
      proxyProps.setProxyPort(proxyPort);
      HttpClientFactory.setProxyProperties(proxyProps);
    }

    // perform discovery on the user-supplied identifier
    List discoveries = _manager.discover(userSuppliedString);

    // attempt to associate with the OpenID provider
    // and retrieve one service endpoint for authentication
    DiscoveryInformation discovered = _manager.associate(discoveries);

    // the next section will figure out where we go next, if anywhere.
    WOActionResults results = null;

    if (discovered != null)
    {
      // store the discovery information in the user's session
      session.setObjectForKey(discovered, EROpenIDManager.DISCOVERY_INFO_KEY);

      // configure the return_to URL where your application will receive
      // the authentication responses from the OpenID provider
      String returnToUrl = _delegate.returnToUrl(request, context);

      // obtain a AuthRequest message to be sent to the OpenID provider
      AuthRequest authReq = _manager.authenticate(discovered, returnToUrl, realm);

      // add the message extensions
      List<MessageExtension> exts = _delegate.createFetchMessageExtensions(userSuppliedString, request, context);
      for (MessageExtension ext : exts) {
        // attach the extension to the authentication request
        EROpenIDManager.log.debug("Authentication request extension: " + ext);
        authReq.addExtension(ext);
      }

      if (!discovered.isVersion2()) {
        WORedirect redirect = new WORedirect(context);
        String url = authReq.getDestinationUrl(true);
        EROpenIDManager.log.debug("Request URL: " + url);
        redirect.setUrl(url);
        results = redirect;
      }
      else {
        String formRedirectionPageName = ERXProperties.stringForKeyWithDefault("er.openid.formRedirectionPageName", EROFormRedirectionPage.class.getName());
        EROFormRedirectionPage formRedirectionPage = (EROFormRedirectionPage)WOApplication.application().pageWithName(formRedirectionPageName, context);
        formRedirectionPage.setParameters( authReq.getParameterMap() );
        String url = authReq.getDestinationUrl(false);
        EROpenIDManager.log.debug("Request URL: " + url);
        formRedirectionPage.takeValueForKey(url, "redirectionUrl");
        results = formRedirectionPage;
      }
    }

    return results;
  }

  /**
   * The callback for verifying the OpenID response.
   * 
   * @param request the WORequest
   * @param context the WOContext
   * @return the OpenID response
   * @throws MessageException
   * @throws DiscoveryException
   * @throws AssociationException
   */
  public EROResponse verifyResponse(WORequest request, WOContext context) throws MessageException, DiscoveryException, AssociationException {

    WOSession session = context.session();

    // extract the parameters from the authentication response
    // (which comes in as a HTTP request from the OpenID provider)
    ParameterList responseParameters = new ParameterList();
    Enumeration formValueKeyEnum = request.formValueKeys().objectEnumerator();
    while (formValueKeyEnum.hasMoreElements()) {
      String formValueKey = (String) formValueKeyEnum.nextElement();
      String formValue = request.stringFormValueForKey(formValueKey);
      responseParameters.set(new Parameter(formValueKey, formValue));
      EROpenIDManager.log.debug("Response parameter: " + formValueKey + " => " + formValue);
    }

    // retrieve the previously stored discovery information
    DiscoveryInformation discovered = (DiscoveryInformation) session.objectForKey(EROpenIDManager.DISCOVERY_INFO_KEY);

    // extract the receiving URL from the HTTP request

    String receivingUrl = _delegate.rewriteReceivingUrl(request, context);

    // verify the response; ConsumerManager needs to be the same
    // (static) instance used to place the authentication request
    VerificationResult verification = _manager.verify(receivingUrl, responseParameters, discovered);

    // examine the verification result and extract the verified identifier
    FetchResponse fetchResponse = null;
    List<MessageExtension> messageExtensions = new ArrayList<MessageExtension>();
    Identifier identifier = verification.getVerifiedId();
    if (identifier != null) {
      AuthSuccess authSuccess = AuthSuccess.createAuthSuccess(responseParameters);
      EROpenIDManager.log.debug("AuthSucess:" + authSuccess);

      if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
        MessageExtension ext = authSuccess.getExtension(AxMessage.OPENID_NS_AX);
        messageExtensions.add(ext);
        EROpenIDManager.log.debug("MessageExtension (AX):" + ext);
        // handle backwards, deprecated compatibility
        if (ext instanceof FetchResponse && fetchResponse == null)
          fetchResponse = (FetchResponse)ext;
      }

      if (authSuccess.hasExtension(SRegMessage.OPENID_NS_SREG)) {
        MessageExtension ext = authSuccess.getExtension(SRegMessage.OPENID_NS_SREG);
        messageExtensions.add(ext);
        EROpenIDManager.log.debug("MessageExtension (SREG):" + ext);
      } 

      if (authSuccess.hasExtension(SRegMessage.OPENID_NS_SREG11)) {
        MessageExtension ext = authSuccess.getExtension(SRegMessage.OPENID_NS_SREG11);
        messageExtensions.add(ext);
        EROpenIDManager.log.debug("MessageExtension (SREG11):" + ext);
      } 
    }

    EROResponse eroResponse = new EROResponse(identifier, fetchResponse, messageExtensions);
    _delegate.responseReceived(verification, eroResponse, request, context);
    return eroResponse;
  }
}
