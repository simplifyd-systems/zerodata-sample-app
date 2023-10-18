# Zerodata SDK

The zerodata sdk allows businesses to serve content to their users data free over the internet.

[![](https://jitpack.io/v/simplifyd-systems/android-zerodata-sdk.svg)](https://jitpack.io/#simplifyd-systems/android-zerodata-sdk)

## Installation

### Option 1: Installation from jitpack.io
Add it in your root build.gradle at the end of repositories:
```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
Add the following dependency to your app's build.gradle file:
```groovy
dependencies {
    implementation 'com.github.simplifyd-systems:android-zerodata-sdk:Tag' //find the tag for the latest version above
}
```

### Option 2: Installation from aar artifact
Add the following dependency to your app's build.gradle file:
- Download the SDK artifact from latest version on the [releases](https://github.com/simplifyd-systems/zerodata-sample-app/releases) page.
- Add the `zerodata-sdk-release.aar` file to the root directory of your project
- Add the following dependency to your app's build.gradle file:
```groovy
dependencies {
     implementation path(":zerodata-sdk-release.aar")
}
```

## Initialization
### Step 1: Obtain API keys
- Obtain the `appToken` from the Simplifyd dashboard
- Obtain the `apiKey` from the Simplifyd dashboard

### Step 2.1: Instantiate and Initialize SDK
- In your root activity class or application class, initialize the SDK using the following lines of code:
```kotlin
ZeroData.sdk.initialize(apiKey, isProduction) //isProduction should be set to true or false depending on if the app is in the production enviroment
```

### Step 2.2: Register for a session token and userId
You need to register the current user of the app so their traffic and zerodata balance can be uniquely identified and measured on zerodata hence the need for the `userId` and `sessionToken`.
- The userId is used to identify the current user on zerodata's servers, you can keep this in encryptedSharedPreferences or in a singleton in memory store that is available app wide.
- The sessionToken is used for all subsequent interactions with the zerodata servers, you can keep this in encryptedSharedPreferences or in a singleton in memory store that is available app wide.

To obtain these values; invoke the following lines of code in your application class, root activity class or immediately after your user is logged in (in case your app contains a login/registration flow):
```kotlin
ZeroData.sdk.registerUser(
    USER_IDENTIFIER, //USER_IDENTIFIER can be anything that uniquely identifies this user on your systems e.g phonenumber, email, userId
    APP_TOKEN, //APP_TOKEN can be obtained from the zero data dashboard during setup. Make sure to keep this securely, it is a private key that is used to ensure traffic on your account is from your app
    object : ZeroDataRegistrationListener {
        override fun onSuccess(uuid: UUID?, sessionToken: String) {
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "UserId is available $uuid",
                    Toast.LENGTH_SHORT
                ).show()
            }
            this@MainActivity.userId = uuid
            this@MainActivity.sessionToken = sessionToken 
        }

        override fun onFailure(throwable: Throwable?) {
            throwable?.let {
                throwable.printStackTrace()
                showErrorToastMessage(throwable)
            }

            binding.simpleSwitch.isChecked = false
        }

    })
```

### Step 3.1: Start connection
- At the point where you need to connect to the SDK, add the following snippet, lets assume your root activity is called MainActivity.
```kotlin
ZeroData.sdk.startConnection(this, MainActivity::class.java, userId, sessionToken)
```
Remember to change MainActivity to match the name of the Activity that is initializing the SDK

### Step 3.2: Listen for connection status changes
- To get updates on connection status changes, add the following snippets
```kotlin
ZeroData.sdk.addListener(object : ZeroDataConnectionListener {
    override fun onConnected() {
        // show connected UI
    }

    override fun onStartConnection() {
        //this line should remain unchanged, 
        // it enables the SDK to request for VPN permissions
        // from the system during first launch
        
        startService(ZeroData.sdk.getConnectServiceIntent(this@MainActivity))
    }

    override fun onTimeTicked(time: String?) {
        ZeroData.sdk.connectionStatus.getOrDefault(CONNECTED).let {
            // update ticker UI
        }
    }

    override fun onConnecting() {
        // show connecting UI
    }

    override fun onUrlAvailable(url: String?) {
        // open browser URL
    }

    override fun onDisconnected() {
        ZeroData.sdk.connectionStatus.getOrDefault(DISCONNECTED).let {
            // show disconnected UI
        }
    }

    override fun onFailure(throwable: Throwable) {
        // show failed UI
    }
})
```

```kotlin
 override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ZeroData.VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startService(ZeroData.sdk.getConnectServiceIntent(this))
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
 }
```

### Step 4: Disconnect when session is complete
- To disconnect from the VPN, add the following snippet
```kotlin
 ZeroData.sdk.stopConnection(this, sessionToken)
```

## FAQs
- Why do I need to pass the sessionToken everytime I call an SDK function? : Some apps rely on constantly rotating tokens
that are generated at runtime for security, hence a new token has to be supplied for each call to zerodata, if this is not the case
for your implementation, feel free to use the same token all through the session.
- What is the Zerodata is connected notification that shows up when a session is ongoing? : This is a notification indicating a foreground service 
that keeps the VPN connection alive for as long as needed, no extra battery power is drawn as a result.
