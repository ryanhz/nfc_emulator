# NFC Emulator

A Flutter plugin to emulate the NFC tag. Supported only on Android (Apple is being Apple).

## Installing

Add to pubspec.yaml:

```
dependencies:
  nfc_enumator: <latest version>
```

## Android Setup

Add NFC permissions to Android package's manifest file `AndroidManifest.xml`:

```
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="false" />
<uses-feature android:name="android.hardware.nfc.hce" android:required="false" />
<uses-permission android:name="android.permission.VIBRATE" />
```

And `aid_list.xml` to the `android/app/src/main/res/xml` folder (create if not exist). See `example project`

<details><summary>File: aid_list.xml</summary>
```
<?xml version="1.0" encoding="utf-8"?>
<host-apdu-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:requireDeviceUnlock="false">
    <aid-group android:category="other">
        <aid-filter android:name="<Replace with your AID>"/>
    </aid-group>
</host-apdu-service>
```

Add emulator service with such metadata to `AndroidManifest.xml`:
```
<service
	android:name="io.flutter.plugins.nfc_emulator.NfcEmulatorService"
	android:exported="true"
	android:permission="android.permission.BIND_NFC_SERVICE">

	<!-- Intent filter indicating that we support card emulation. -->
	<intent-filter>
		<action android:name="android.nfc.cardemulation.action.HOST_APDU_SERVICE" />
		<category android:name="android.intent.category.DEFAULT" />
	</intent-filter>
	<!--
			Required XML configuration file, listing the AIDs that we are emulating cards
			for. This defines what protocols our card emulation service supports.
	-->
	<meta-data
		android:name="android.nfc.cardemulation.host_apdu_service"
		android:resource="@xml/aid_list" />
</service>
```

## Example of Usage

### Check NFC Status:

``` dart
// Check NFC Status
int nfcStatus = await NfcEmulator.nfcStatus;
```

### Start NFC Emulator

``` dart
// Start NFC emulator with AID, cardUid, and optional AES key:
await NfcEmulator.startNfcEmulator("666B65630001", "cd22c716", "79e64d05ed6475d3acf405d6a9cd506b");
```

### Stop NFC Emulator

``` dart
// Stop NFC emulator:
await NfcEmulator.stopNfcEmulator();
```

**See more at**: [example project](example/lib/main.dart)