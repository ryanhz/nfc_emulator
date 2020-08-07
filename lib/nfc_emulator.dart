
import 'dart:async';

import 'package:flutter/services.dart';

class NfcEmulator {
  static const MethodChannel _channel =
      const MethodChannel('nfc_emulator');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<int> get nfcStatus async {
    final int status = await _channel.invokeMethod('getNfcStatus');
    return status;
  }

  static Future<void> startNfcEmulator(String cardAid, String cardUid, [String aesKey]) async {
    await _channel.invokeMethod('startNfcEmulator', {
        "cardAid": cardAid,
        "cardUid": cardUid,
        "aesKey": aesKey,
      });
  }

  static Future<void> stopNfcEmulator() async {
    await _channel.invokeMethod('stopNfcEmulator');
  }

}
