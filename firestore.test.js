const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read games", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("games").get());
});

test("Authenticated user: cannot read another user's games", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("games").doc("zelda").set({
      userId: BOB_UID,
      title: "The Legend of Zelda",
      platform: "NES",
      isWishlist: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("games").doc("zelda").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("games").get());
});

test("Authenticated user: can create a valid game in own collection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("games").doc("mario64").set({
      userId: ALICE_UID,
      title: "Super Mario 64",
      platform: "Nintendo 64",
      genre: "Plateforme 3D",
      releaseYear: 1996,
      publisher: "Nintendo",
      format: "Cartouche",
      region: "PAL",
      barcode: "045496870027",
      isWishlist: false,
      boxCondition: "Très bon état",
      manualCondition: "Présent & Parfait",
      mediaCondition: "Parfait / Mint",
      completeness: "CIB",
      purchasePrice: 45.0,
      estimatedValue: 65.0,
      purchaseDate: "2024-01-15",
      rating: 5,
      status: "Terminé",
      notes: "Version française boîte noire",
      coverUrl: "",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot spoof userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("games").doc("spoof").set({
      userId: BOB_UID, // Mismatch
      title: "Super Mario 64",
      platform: "Nintendo 64",
      isWishlist: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot inject illegal fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("games").doc("hack").set({
      userId: ALICE_UID,
      title: "Super Mario 64",
      platform: "Nintendo 64",
      isWishlist: false,
      adminField: true, // Ghost field
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can update own game without mutating createdAt", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const createdDate = new Date();

  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("games").doc("mario64").set({
      userId: ALICE_UID,
      title: "Super Mario 64",
      platform: "Nintendo 64",
      isWishlist: false,
      rating: 4,
      createdAt: createdDate,
      updatedAt: createdDate,
    });
  });

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("games").doc("mario64").update({
      rating: 5,
      notes: "Rejoué ce week-end, chef-d'œuvre absolu",
      updatedAt: new Date(),
    })
  );

  // Fails if attempting to change createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("games").doc("mario64").update({
      createdAt: new Date(2000, 1, 1),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can delete own game", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("games").doc("mario64").set({
      userId: ALICE_UID,
      title: "Super Mario 64",
      platform: "Nintendo 64",
      isWishlist: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("games").doc("mario64").delete()
  );
});
