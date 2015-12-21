
import java.security.Key
import javax.crypto.SecretKey
import javax.crypto.KeyGenerator
import java.security.KeyPair
import java.security.KeyPairGenerator
import javax.crypto.Cipher

SecretKey newSecretKey(String algorithm) {
    KeyGenerator.getInstance(algorithm).generateKey()
}

KeyPair newKeyPair(String algorithm) {
    KeyPairGenerator.getInstance(algorithm).generateKeyPair()
}

byte[] encrypt(Key key, byte[] plaintext) throws Exception {
    return Cipher.getInstance(key.algorithm).with {
        init Cipher.ENCRYPT_MODE, key
        doFinal plaintext
    }
}

byte[] decrypt(Key key, byte[] ciphertext) throws Exception {
    Cipher.getInstance(key.algorithm).with {
        init Cipher.DECRYPT_MODE, key
        doFinal ciphertext
    }
}

def printKey(Key key, String title) {
    key.with {
        println """
        ${title}:
            algorithm: $algorithm
            format: $format
            encoded: $encoded
            encoded.class: ${encoded.class}
        """.stripIndent()
    }
}

Map encryptAndDecrypt(Key encryptKey, Key decryptKey, byte[] plaintext) {
    def result = [:]
    result.plaintext = plaintext
    result.encryptKey = encryptKey
    result.decryptKey = decryptKey
    result.ciphertext = encrypt result.encryptKey, result.plaintext
    result.decrypted = decrypt result.decryptKey, result.ciphertext
    return result
}

def tryEncryptAndDecrypt(Key encryptKey, Key decryptKey, String testString) {
    def result = encryptAndDecrypt(encryptKey, decryptKey, testString.bytes)
    result.origin = testString
    result.decryptedTex = new String(result.decrypted)

    println "algorithm: $encryptKey.algorithm\n"
    //println result
    result.each { key, value -> println "${key}:\t ${value}" }
    printKey result.encryptKey, 'encryptKey'
    printKey result.decryptKey, 'decryptKey'
}


println '---------------------'
//def key = newSecretKey 'AES'
//tryEncryptAndDecrypt(key, key, 'Hello World!')
newSecretKey 'AES' with { key -> 1.times { println "--- round $it ---"; tryEncryptAndDecrypt key, key, 'Hello World!' } }

println '---------------------'
newSecretKey 'DES' with { tryEncryptAndDecrypt it, it, 'Hello World!' }

println '---------------------'
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec
import java.security.spec.RSAPrivateKeySpec
import java.security.spec.X509EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
newKeyPair 'RSA' with {
    tryEncryptAndDecrypt it.'public', it.'private', 'Hello World!'


    /*println 'show Key:'
    it.'public'.with { println "modulus: ${modulus}\npublicExponent: ${publicExponent}" }
    it.'private'.with { println "modulus: ${modulus}\nprivateExponent: ${privateExponent}" }
    def keyFactory = KeyFactory.getInstance('RSA')
    println '\nshow KeySpec:'
    keyFactory.getKeySpec(it.'public', RSAPublicKeySpec).with { println "modulus: ${modulus}\npublicExponent: ${publicExponent}" }
    keyFactory.getKeySpec(it.'private', RSAPrivateKeySpec).with { println "modulus: ${modulus}\nprivateExponent: ${privateExponent}" }

    new X509EncodedKeySpec(it.'public'.encoded).with {
        println format; println encoded; println it.class
        keyFactory.generatePublic(it).with {
            println "from encoded to Key: $it"
        }
    }
    new PKCS8EncodedKeySpec(it.'private'.encoded).with {
        println format; println encoded; println it.class
        keyFactory.generatePrivate(it).with {
            println "from encoded to Key: $it"
        }
    }*/

    /*println '# Change PublicExponent'
    def changedPublicKey = new RSAPublicKeySpec(it.'public'.modulus+1,  it.'public'.publicExponent)
    changedPublicKey = KeyFactory.getInstance('RSA').generatePublic(changedPublicKey)
    println "changedPublicKey: $changedPublicKey "
    tryEncryptAndDecrypt changedPublicKey, it.'private', 'Hello World!'*/
}




// encrypt image

println '---------------------'
println 'encrypt image\n'

import javax.imageio.ImageIO
import java.nio.ByteBuffer

{ ->

def image = ImageIO.read(new File('origin.png'))
println "width: ${image.width}\nheight: ${image.height}"
int[] imageInts = new int[image.width * image.height]
image.getRGB(0, 0, image.width, image.height, imageInts, 0, image.width)
def buffer = ByteBuffer.allocate(image.width * image.height * 4)
buffer.asIntBuffer().put(imageInts)

println 'write before_encrypte.png'
ImageIO.write(image, "png", new File('before_encrypte.png'))

def key = newSecretKey 'AES'
println 'encrypt and decrypt'
def result = encryptAndDecrypt(key, key, buffer.array())

buffer.clear()
buffer.put(result.ciphertext, 0, buffer.capacity())
buffer.flip()
buffer.asIntBuffer().get(imageInts)
image.setRGB(0, 0, image.width, image.height, imageInts, 0, image.width)
println 'write encrypted.png'
ImageIO.write(image, "png", new File('encrypted.png'))

buffer.clear()
buffer.put(result.decrypted, 0, buffer.capacity())
buffer.flip()
buffer.asIntBuffer().get(imageInts)
image.setRGB(0, 0, image.width, image.height, imageInts, 0, image.width)
println 'write decrypted.png'
ImageIO.write(image, "png", new File('decrypted.png'))

}()

System.gc()
