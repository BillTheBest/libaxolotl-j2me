package org.whispersystems.libaxolotl.groups;

import junit.framework.TestCase;
import org.whispersystems.libaxolotl.DuplicateMessageException;
import org.whispersystems.libaxolotl.InvalidMessageException;
import org.whispersystems.libaxolotl.LegacyMessageException;
import org.whispersystems.libaxolotl.NoSessionException;
import org.whispersystems.libaxolotl.j2me.AssertionError;
import org.whispersystems.libaxolotl.j2me.FakeSecureRandomProvider;
import org.whispersystems.libaxolotl.protocol.SenderKeyDistributionMessage;

import java.util.Vector;

public class GroupCipherTest extends TestCase {

  public GroupCipherTest(String name) {
    super(name);
  }

  public void testBasicEncryptDecrypt()
      throws LegacyMessageException, DuplicateMessageException, InvalidMessageException, NoSessionException
  {
    InMemorySenderKeyStore aliceStore = new InMemorySenderKeyStore();
    InMemorySenderKeyStore bobStore   = new InMemorySenderKeyStore();

    GroupSessionBuilder aliceSessionBuilder = new GroupSessionBuilder(aliceStore);
    GroupSessionBuilder bobSessionBuilder   = new GroupSessionBuilder(bobStore);

    GroupCipher aliceGroupCipher = new GroupCipher(new FakeSecureRandomProvider(), aliceStore, new SenderKeyName("cool group", 1111, 0));
    GroupCipher bobGroupCipher   = new GroupCipher(new FakeSecureRandomProvider(), bobStore, new SenderKeyName("cool group", 1111, 0));

    SenderKeyDistributionMessage aliceDistributionMessage =
        aliceSessionBuilder.create(new SenderKeyName("cool group", 1111, 0), new FakeSecureRandomProvider());

    bobSessionBuilder.process(new SenderKeyName("cool group", 1111, 0), aliceDistributionMessage);

    byte[] ciphertextFromAlice = aliceGroupCipher.encrypt("smert ze smert".getBytes());
    byte[] plaintextFromAlice  = bobGroupCipher.decrypt(ciphertextFromAlice);

    assertTrue(new String(plaintextFromAlice).equals("smert ze smert"));
  }

  public void testBasicRatchet()
      throws LegacyMessageException, DuplicateMessageException, InvalidMessageException, NoSessionException
  {
    InMemorySenderKeyStore aliceStore = new InMemorySenderKeyStore();
    InMemorySenderKeyStore bobStore   = new InMemorySenderKeyStore();

    GroupSessionBuilder aliceSessionBuilder = new GroupSessionBuilder(aliceStore);
    GroupSessionBuilder bobSessionBuilder   = new GroupSessionBuilder(bobStore);

    SenderKeyName aliceName = new SenderKeyName("cool group", 1111, 0);

    GroupCipher aliceGroupCipher = new GroupCipher(new FakeSecureRandomProvider(), aliceStore, aliceName);
    GroupCipher bobGroupCipher   = new GroupCipher(new FakeSecureRandomProvider(), bobStore, aliceName);

    SenderKeyDistributionMessage aliceDistributionMessage =
        aliceSessionBuilder.create(aliceName, new FakeSecureRandomProvider());

    bobSessionBuilder.process(aliceName, aliceDistributionMessage);

    byte[] ciphertextFromAlice  = aliceGroupCipher.encrypt("smert ze smert".getBytes());
    byte[] ciphertextFromAlice2 = aliceGroupCipher.encrypt("smert ze smert2".getBytes());
    byte[] ciphertextFromAlice3 = aliceGroupCipher.encrypt("smert ze smert3".getBytes());

    byte[] plaintextFromAlice   = bobGroupCipher.decrypt(ciphertextFromAlice);

    try {
      bobGroupCipher.decrypt(ciphertextFromAlice);
      throw new AssertionError("Should have ratcheted forward!");
    } catch (DuplicateMessageException dme) {
      // good
    }

    byte[] plaintextFromAlice2  = bobGroupCipher.decrypt(ciphertextFromAlice2);
    byte[] plaintextFromAlice3  = bobGroupCipher.decrypt(ciphertextFromAlice3);

    assertTrue(new String(plaintextFromAlice).equals("smert ze smert"));
    assertTrue(new String(plaintextFromAlice2).equals("smert ze smert2"));
    assertTrue(new String(plaintextFromAlice3).equals("smert ze smert3"));
  }

  public void testOutOfOrder()
      throws LegacyMessageException, DuplicateMessageException, InvalidMessageException, NoSessionException
  {
    InMemorySenderKeyStore aliceStore = new InMemorySenderKeyStore();
    InMemorySenderKeyStore bobStore   = new InMemorySenderKeyStore();

    GroupSessionBuilder aliceSessionBuilder = new GroupSessionBuilder(aliceStore);
    GroupSessionBuilder bobSessionBuilder   = new GroupSessionBuilder(bobStore);

    SenderKeyName aliceName = new SenderKeyName("cool group", 1111, 0);

    GroupCipher aliceGroupCipher = new GroupCipher(new FakeSecureRandomProvider(), aliceStore, aliceName);
    GroupCipher bobGroupCipher   = new GroupCipher(new FakeSecureRandomProvider(), bobStore, aliceName);

    SenderKeyDistributionMessage aliceDistributionMessage =
        aliceSessionBuilder.create(aliceName, new FakeSecureRandomProvider());

    bobSessionBuilder.process(aliceName, aliceDistributionMessage);

    Vector ciphertexts = new Vector(100);

    for (int i=0;i<100;i++) {
      ciphertexts.addElement(aliceGroupCipher.encrypt("up the punks".getBytes()));
    }

    while (ciphertexts.size() > 0) {
      int    index      = randomInt() % ciphertexts.size();
      byte[] ciphertext = (byte[])ciphertexts.elementAt(index);
      ciphertexts.removeElementAt(index);
      byte[] plaintext  = bobGroupCipher.decrypt(ciphertext);

      assertTrue(new String(plaintext).equals("up the punks"));
    }
  }

  public void testEncryptNoSession() {
    InMemorySenderKeyStore aliceStore = new InMemorySenderKeyStore();
    GroupCipher aliceGroupCipher = new GroupCipher(new FakeSecureRandomProvider(), aliceStore, new SenderKeyName("coolio groupio", 1111, 0));
    try {
      aliceGroupCipher.encrypt("up the punks".getBytes());
      throw new AssertionError("Should have failed!");
    } catch (NoSessionException nse) {
      // good
    }
  }

  private int randomInt() {
    return new FakeSecureRandomProvider().nextInt(Integer.MAX_VALUE);
  }
}
