#Nymi Reference Apps
This project contains apps that demonstrate how to interact with the Nymi Band via the Nymi Band Android SDK.
All references to a Nymi Band must be understood as an authenticated Nymi Band.

##nymireferenceapp 
This app demonstrates interacting with a Nymi Band that acts as a bound authenticator.

* Provision a Nymi Band
* Send haptic notifications to a provisioned Nymi Band
* Request pseodo-random numbers from a provisioned Nymi Band
* Request TOTP tokens (Time-based one time passwords) from a provisioned Nymi Band
* Request message signing from a provisioned Nymi Band, using the ECDSA signing algorithm
* Request symmetric keys from a provisioned Nymi Band
* Query or receive notifications on the presence state of a provisioned Nymi Band
* Receive notifications on all detected Nymi Bands in bluetooth transmission range of the NEA
* Register a Nymi Band for roaming authentication

##nymireferenceterminal 
This app demonstrates interacting with an unprovisioned Nymi Band that acts as a roaming authenticator.
The prerequisite to using this app is that the Nymi Band has been registered for roaming authentication, 
and a remote server implementing the roaming authentication protocol exists. See the developer docs on
details on this protocol.

#Getting Started

##Setting up the environment

* Clone the repository to your machine.  
* Open the project with your Android Studio.  
* Rebuild the project.
* For the general sample app, make sure you run **NymiReferenceApp**, not NymiReferenceTerminal.

Note that by default, the sample apps build with the Nymulator library.

## Using the Sample App with the Nymulator

* Make sure you link to the Nymulator flavour of the Nymi Api library. In nymireferenceapp/build.gradle and/or 
nymireferenceterminal/build.gradle, you should have `compile project(':nymi-api-nymulator')` in your dependencies structure.
* Make sure your Android device and the host machine running the Nymulator are on the same WIFI network.
* Inform your Android project of the IP address of the machine running Nymulator. You should set this IP 
by calling `mNymiAdapter.setNymulator(nymulatorHost);` where `nymulatorHost` is a string with the following example 
format "10.0.1.91". Nymulator IP must be set before initialization of NymiAdaptor. A suggested place for this is in
the `onCreate(...)` function.
* Start the Nymulator, and run your Android app.

## Using Sample App with the Nymi Band

* Make sure you link to the Nymi band flavour of the api library. In nymireferenceapp/build.gradle and/or 
nymireferenceterminal/build.gradle, you should have `compile project(':nymi-api')` in your dependencies structure.
3. Rebuild your project.

#General Usage and API

## Setup and initialization

1. Get an instance of the NymiAdapter. 
```java
mNymiAdapter = NymiAdapter.getDefaultAdapter();
```

2. If developing with the Nymulator, set Nymulator IP. This must be done before Nymi Api initialization. This step is
irrelevant if you are connecting to the physical Nymi Band. 
```java
if (!mNymiAdapter.isInitialized()) {
...     
// NOTE: we cannot always use BuildConfig.LOCAL_IP to set the IP since the host can have 
// a few network interfaces, and the interface could be bound to more than one IP address.
// In typical dev environments, the nymulator will be run on the
// same machine as your build machine, so this is a sensible default.
// If Nymulator is not reachable through BuildConfig.LOCAL_IP, use the actual IP address of the machine.
String nymulatorHost = BuildConfig.LOCAL_IP;
mNymiAdapter.setNymulator(nymulatorHost);
....
}
```
3. Initialize the Nymi adapter.  Receive notification of when the NymiAdapter has been initialized via NymiAdapter.InitCallback.onInitResult()
```java
if (!mNymiAdapter.isInitialized()) {
...
// Context supplied to NymiAdapter here will be preserved throughout the lifetime of the app
mNymiAdapter.init(ApplicationContextProvider.getContext(),
BuildConfig.APPLICATION_ID, new NymiAdapter.InitCallback() {
@Override
public void onInitResult(int status, String message) {
...
}
});
...
}
```

## Provision handling

### Retrieve existing provisions

1.  Provisions are available only if the NymiAdapter has been initialized.
```java
if (!mNymiAdapter.isInitialized()) {
...
mNymiAdapter.init(ApplicationContextProvider.getContext(),
BuildConfig.APPLICATION_ID, new NymiAdapter.InitCallback() {
@Override
public void onInitResult(int status, String message) {
Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
if (status == NymiAdapter.InitCallback.INIT_SUCCESS) {
/** NymiAdapter just initialized. Now retrieve any existing provisions */
mAdapterProvisions.setProvisions(
NymiAdapter.getDefaultAdapter().getProvisions());
} else if (status == NymiAdapter.InitCallback.INIT_FAIL) {
...
}
}
});
} else {
/** NymiAdapter is already initialized. Retrieve any existing provisions */
mAdapterProvisions.setProvisions(NymiAdapter.getDefaultAdapter().getProvisions());
}
```

2.  Update the UI with the provisions
```java
mListViewProvisions.setAdapter(mAdapterProvisions);
```

### Provision new Nymi Bands

1.  Set callbacks to receive agreement and new provision events
```java
mNymiAdapter.setAgreementCallback(new NymiAdapter.AgreementCallback() {
@Override
public void onAgreement(BitSet pattern) {
// Present pattern to the user here.
// If user confirms pattern, call mNymiAdapter.setPattern
}
});
mNymiAdapter.setNewProvisionCallback(new NymiAdapter.NewProvisionCallback() {
@Override
public void onNewProvision(int status, NymiProvision provision) {
if (status == NymiAdapter.NewProvisionCallback.PROVISION_SUCCESS) {
// New provision obtained, save its id here
} else {
// Handle error here
}
}
});
```

2.  Start scanning for nearby Nymi Bands that are in provisioning mode.
```java
mNymiAdapter.startProvision();
```

## Set notification callbacks

Notification callbacks allow the Nymi Api to notify your app when certain events take place. This example sets a callback 
for notifications whenever a Nymi band is detected.

```java
mNymiAdapter.setDeviceDetectedCallback(new NymiAdapter.DeviceDetectedCallback() {
@Override
public void onDeviceDetected(String provisionId, boolean connectable, int rssi, int smoothedRssi) {
Log.d(LOG_TAG, "onDeviceDetected provisionId=" + provisionId +
" connectable=" + connectable +
" rssi=" + rssi +
" smoothedRssi=" + smoothedRssi);
}
});
```

## Resource cleanup

In your onDestroy() method, call mNymiAdapter.clearCallbacks().  Some might contain references to Views, this step is needed to avoid memory leaks
```java
@Override
protected void onDestroy() {
super.onDestroy();
/** Clear callbacks that contain references to Views to avoid memory leaks */
mNymiAdapter.clearCallbacks();
}
```

## Known bugs

1. If the Nymi Band (virtual or physical) unclasps and reclasps during the lifetime of an NEA, the NEA will not be able to communicate to the Nymi Band until the NEA and Nymi API are restarted.
2. ```java NymiAdapter.NewAdvNonceCallback() ``` is currently unstable and results in sporadic crashes.

