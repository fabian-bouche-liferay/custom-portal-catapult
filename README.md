# README

This project contains an override module of PortalCatapult (the component in charge of making calls against
client extension endpoints) so that it forwards the OIDC **ID Token** to the client extension.

## Setup

Create a file called `com.liferay.portal.component.blacklist.internal.configuration.ComponentBlacklistConfiguration.config` with this contents:

```
blacklistComponentNames=[ \
  "com.liferay.portal.catapult.internal.PortalCatapultImpl", \
  ]
```

Deploy it inside of the Liferay bundle's `osgi/configs`.

## Pre-requisite

Setup some OpenId Provider in Liferay.

I'd suggest to use Keycloak.

## Testing

Build and deploy (`blade gw clean deploy`) this client extension:

https://github.com/fabian-bouche-liferay/custom-portal-catapult/tree/2025.Q2/client-extensions/energy-proxy-object

And start it with `yarn start`.

This is the implementation of an objectEntryManager client extension (aka proxy object).

Enable the Proxy Object feature flag.

Import this object definition [Object_Definition_EnergyConsumption_33605_20260617115022229.json](Object_Definition_EnergyConsumption_33605_20260617115022229.json)

Now go to Control Panel -> Objects -> Energy Consumptions

It prints the table of records returned by the objectEntryManager CX.

If the user logged in using OIDC, then Liferay will forward the OIDC **ID Token** to the CX.

The CX prints logs so that you can monitor this.
