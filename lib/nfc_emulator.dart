import 'dart:async';

import 'package:flutter/services.dart';

enum NfcStatus { unknown, enabled, notSupported, notEnabled }

class NfcEmulator {
  static const MethodChannel _channel = const MethodChannel('nfc_emulator');

  /*
   * Get platform version
   */
  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  /*
   * Get NFC status
   */
  static Future<NfcStatus> get nfcStatus async {
    final int? status = await _channel.invokeMethod('getNfcStatus');
    return _parseNfcStatus(status);
  }

  /*
   * Start NFC Emulator
   * cardAid: Card AID, for example: 666B65630001
   * cardUid: Card UID, for example: cd22c716
   * aesKey: AES key to encrypt, optional, 16 bytes (hex length 32)
   */
  static Future<void> startNfcEmulator(String cardAid, String cardUid,
      [String? aesKey]) async {
    await _channel.invokeMethod('startNfcEmulator', {
      "cardAid": cardAid,
      "cardUid": cardUid,
      "aesKey": aesKey,
    });
  }

  /*
   * Stop NFC Emulator
   */
  static Future<void> stopNfcEmulator() async {
    await _channel.invokeMethod('stopNfcEmulator');
  }

  static NfcStatus _parseNfcStatus(int? value) {
    switch (value) {
      case -1:
        return NfcStatus.unknown;
      case 0:
        return NfcStatus.enabled;
      case 1:
        return NfcStatus.notSupported;
      case 2:
        return NfcStatus.notEnabled;
      default:
        return NfcStatus.unknown;
    }
  }
}
