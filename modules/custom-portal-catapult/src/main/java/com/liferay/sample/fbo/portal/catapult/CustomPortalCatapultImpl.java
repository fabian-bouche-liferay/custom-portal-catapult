/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.sample.fbo.portal.catapult;

import com.liferay.oauth.client.LocalOAuthClient;
import com.liferay.oauth2.provider.model.OAuth2Application;
import com.liferay.oauth2.provider.service.OAuth2ApplicationLocalService;
import com.liferay.petra.executor.PortalExecutorManager;
import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.catapult.PortalCatapult;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.security.sso.openid.connect.constants.OpenIdConnectWebKeys;
import com.liferay.portal.security.sso.openid.connect.persistence.model.OpenIdConnectSession;
import com.liferay.portal.security.sso.openid.connect.persistence.service.OpenIdConnectSessionLocalService;

import java.io.IOException;

import java.net.URI;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Brian Wing Shun Chan
 * @author Raymond Augé
 * @author Fabian Bouché
 */
@Component(
	property = {
		"portal.catapult.override=true",
		"service.ranking:Integer=1000"
	},
	service = PortalCatapult.class
)
public class CustomPortalCatapultImpl implements PortalCatapult {

	@Override
	public Future<byte[]> launch(
			long companyId, Http.Method method,
			String oAuth2ApplicationExternalReferenceCode,
			JSONObject payloadJSONObject, String resourcePath, long userId)
		throws PortalException {

		Http.Options options = new Http.Options();

		options.addHeader(
			HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_JSON);

		if (payloadJSONObject != null) {
			options.setBody(
				payloadJSONObject.toString(), ContentTypes.APPLICATION_JSON,
				StringPool.UTF8);
		}

		OAuth2Application oAuth2Application =
			_oAuth2ApplicationLocalService.
				getOAuth2ApplicationByExternalReferenceCode(
					oAuth2ApplicationExternalReferenceCode, companyId);

		String location = _getLocation(oAuth2Application, resourcePath);

		options.setLocation(location);
		options.setMethod(method);

		try {
			TransactionInvokerUtil.invoke(
				_transactionConfig,
				() -> {
					_localOAuthClient.consumeAccessToken(
						accessToken -> options.addHeader(
							HttpHeaders.AUTHORIZATION,
							"Bearer " + accessToken),
						oAuth2Application, userId);

					_addUpstreamIdTokenHeader(
						options, oAuth2Application, location);

					return null;
				});
		}
		catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}

		ExecutorService executorService =
			_portalExecutorManager.getPortalExecutor(
				CustomPortalCatapultImpl.class.getName());

		return executorService.submit(
			() -> {
				try {
					return _http.URLtoByteArray(options);
				}
				catch (IOException ioException) {
					_log.error(ioException);

					return ReflectionUtil.throwException(ioException);
				}
			});
	}

	private void _addUpstreamIdTokenHeader(
		Http.Options options, OAuth2Application oAuth2Application,
		String location) {

		if (!_isAllowedUpstreamLocation(oAuth2Application, location)) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Not forwarding upstream ID token to unauthorized " +
						"location: " + location);
			}

			return;
		}

		try {
			ServiceContext serviceContext =
				ServiceContextThreadLocal.getServiceContext();

			if (serviceContext == null) {
				if (_log.isDebugEnabled()) {
					_log.debug("No ServiceContext available");
				}

				return;
			}

			HttpServletRequest httpServletRequest = serviceContext.getRequest();

			if (httpServletRequest == null) {
				if (_log.isDebugEnabled()) {
					_log.debug("No HttpServletRequest available");
				}

				return;
			}

			HttpSession httpSession = httpServletRequest.getSession(false);

			if (httpSession == null) {
				if (_log.isDebugEnabled()) {
					_log.debug("No HttpSession available");
				}

				return;
			}

			Object openIdConnectSessionIdObject = httpSession.getAttribute(
				OpenIdConnectWebKeys.OPEN_ID_CONNECT_SESSION_ID);

			if (!(openIdConnectSessionIdObject instanceof Long)) {
				if (_log.isDebugEnabled()) {
					_log.debug("No valid OpenID Connect session id found");
				}

				return;
			}

			OpenIdConnectSession openIdConnectSession =
				_openIdConnectSessionLocalService.getOpenIdConnectSession(
					(Long)openIdConnectSessionIdObject);

			String idToken = openIdConnectSession.getIdToken();

			if ((idToken == null) || idToken.isEmpty()) {
				if (_log.isDebugEnabled()) {
					_log.debug("OIDC id_token is empty");
				}

				return;
			}

			options.addHeader("X-Upstream-ID-Token", idToken);
		}
		catch (PortalException portalException) {
			_log.warn("Unable to load OpenID Connect session", portalException);
		}
	}

	private int _getPort(URI uri) {
		int port = uri.getPort();

		if (port != -1) {
			return port;
		}

		String scheme = uri.getScheme();

		if ("http".equalsIgnoreCase(scheme)) {
			return 80;
		}

		if ("https".equalsIgnoreCase(scheme)) {
			return 443;
		}

		return -1;
	}

	private boolean _isAllowedUpstreamLocation(
		OAuth2Application oAuth2Application, String location) {

		try {
			URI homePageURI = new URI(oAuth2Application.getHomePageURL());
			URI locationURI = new URI(location);

			if (!String.valueOf(homePageURI.getScheme()).equalsIgnoreCase(
					locationURI.getScheme())) {

				return false;
			}

			if (!String.valueOf(homePageURI.getHost()).equalsIgnoreCase(
					locationURI.getHost())) {

				return false;
			}

			if (_getPort(homePageURI) != _getPort(locationURI)) {
				return false;
			}

			String homePagePath = homePageURI.getPath();

			if ((homePagePath == null) || homePagePath.isEmpty()) {
				homePagePath = StringPool.SLASH;
			}

			String locationPath = locationURI.getPath();

			if ((locationPath == null) || locationPath.isEmpty()) {
				locationPath = StringPool.SLASH;
			}

			return locationPath.startsWith(homePagePath);
		}
		catch (Exception exception) {
			_log.warn(
				"Unable to validate upstream location: " + location,
				exception);

			return false;
		}
	}

	private String _getLocation(
		OAuth2Application oAuth2Application, String resourcePath) {

		if (resourcePath.contains(Http.PROTOCOL_DELIMITER)) {
			return resourcePath;
		}

		String homePageURL = oAuth2Application.getHomePageURL();

		if (homePageURL.endsWith(StringPool.SLASH)) {
			homePageURL = homePageURL.substring(0, homePageURL.length() - 1);
		}

		if (resourcePath.startsWith(StringPool.SLASH)) {
			resourcePath = resourcePath.substring(1);
		}

		return StringBundler.concat(
			homePageURL, StringPool.SLASH, resourcePath);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CustomPortalCatapultImpl.class);

	private static final TransactionConfig _transactionConfig =
		TransactionConfig.Factory.create(
			Propagation.REQUIRES_NEW, new Class<?>[] {Exception.class});

	@Reference
	private Http _http;

	@Reference
	private LocalOAuthClient _localOAuthClient;

	@Reference
	private OAuth2ApplicationLocalService _oAuth2ApplicationLocalService;

	@Reference
	private OpenIdConnectSessionLocalService
		_openIdConnectSessionLocalService;

	@Reference
	private PortalExecutorManager _portalExecutorManager;

}