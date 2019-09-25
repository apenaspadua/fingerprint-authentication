package com.treinamento.mdomingos.fingerprint

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey


class MainActivity : AppCompatActivity() {

    private var KEY_NAME = "yourKey"
    private var cipher: Cipher? = null
    private var keyStore: KeyStore? = null
    private var keyGenerator: KeyGenerator? = null
    private var cryptoObject: FingerprintManager.CryptoObject? = null
    private var fingerprintManager: FingerprintManager? = null
    private var keyguardManager: KeyguardManager? = null

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager?
            fingerprintManager = getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager
        }

        if(!fingerprintManager?.isHardwareDetected!!){
            textview.text = "Your device doesn't support fingerprint authentication"
        }

        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED){
            textview.text = "Please enable the fingerprint permission"
        }

        if(!fingerprintManager!!.hasEnrolledFingerprints()){
            textview.text = "No fingerprint configured. Please register at least one fingerprint in your device's Settings"
        }

        if(!keyguardManager?.isKeyguardSecure!!){
            textview.text = "Please enable lockscreen security in your device's Settings"

        } else {
            try {
                generateKey()
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }

        if (initCipher()) {
            cryptoObject = FingerprintManager.CryptoObject(cipher)
            val helper = FingerprintHandler(this, this)
            helper.startAuth(fingerprintManager!!, cryptoObject!!)
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun generateKey(){
        try {

            keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")

            keyStore!!.load(null)

            keyGenerator!!.init(
                KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build()
            )

            keyGenerator!!.generateKey()

        } catch (e: KeyStoreException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun initCipher(): Boolean{
        try {

            cipher = Cipher.getInstance(
            KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7)

        } catch (e: java.lang.Exception){
            e.printStackTrace()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                keyStore?.load(null)

                val key = keyStore?.getKey(KEY_NAME, null) as SecretKey
                cipher?.init(Cipher.ENCRYPT_MODE, key)
                return true

            } catch (e: Exception) {
                return false
            } catch (e: KeyStoreException) {
                throw RuntimeException("Failed to init Cipher", e)
            } catch (e: CertificateException) {
                throw RuntimeException("Failed to init Cipher", e)
            } catch (e: UnrecoverableKeyException) {
                throw RuntimeException("Failed to init Cipher", e)
            } catch (e: IOException) {
                throw RuntimeException("Failed to init Cipher", e)
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException("Failed to init Cipher", e)
            } catch (e: InvalidKeyException) {
                throw RuntimeException("Failed to init Cipher", e)
            }
        }

        return false
    }

}
