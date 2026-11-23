const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
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

test("Unauthenticated user: cannot read or write any user collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("cart").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("orders").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("cart").doc("prod_avocado").set({
      userId: ALICE_UID,
      productId: "prod_avocado",
      productName: "Hass Avocados",
      category: "Produce",
      storeType: "GROCERY",
      unitPrice: 4.49,
      unitLabel: "4-pack",
      productLocation: "Westside Market Hub · Aisle 1A",
      quantity: 2,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Cart: owner can create, list, partially update quantity, and delete; cross-user & ghost fields rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  const cartRef = aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("prod_avocado");
  await assertSucceeds(
    cartRef.set({
      userId: ALICE_UID,
      productId: "prod_avocado",
      productName: "Hass Avocados",
      category: "Produce",
      storeType: "GROCERY",
      unitPrice: 4.49,
      unitLabel: "4-pack",
      productLocation: "Westside Market Hub · Aisle 1A",
      quantity: 1,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("cart").get());
  await assertSucceeds(
    cartRef.update({
      quantity: 3,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt cannot change
  await assertFails(
    cartRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // @DocumentId `id` field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("prod_bread").set({
      id: "prod_bread",
      userId: ALICE_UID,
      productId: "prod_bread",
      productName: "Sourdough Loaf",
      category: "Bakery",
      storeType: "GROCERY",
      unitPrice: 5.5,
      unitLabel: "loaf",
      productLocation: "Bakery Hub · Shelf 3",
      quantity: 1,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cart").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cart").doc("prod_avocado").delete());

  await assertSucceeds(cartRef.delete());
});

test("CustomProduct & DeliveryAddress: valid Kotlin payloads succeed; invalid bounds fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  const addrRef = aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_1");
  await assertSucceeds(
    addrRef.set({
      userId: ALICE_UID,
      label: "Home",
      streetAddress: "742 Evergreen Terrace",
      city: "San Francisco",
      instructions: "Leave at front porch",
      latitude: 37.7749,
      longitude: -122.4194,
      defaultSelected: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  const prodRef = aliceDb.collection("users").doc(ALICE_UID).collection("custom_products").doc("custom_1");
  await assertSucceeds(
    prodRef.set({
      userId: ALICE_UID,
      name: "Artisan Honey Jar",
      description: "Raw wildflower honey",
      storeType: "GROCERY",
      category: "Pantry",
      price: 12.99,
      unitLabel: "500g jar",
      locationName: "Central Farmers Stall · Row 4",
      originCity: "Sonoma, CA",
      latitude: 38.2919,
      longitude: -122.458,
      inStock: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Negative price fails
  await assertFails(
    prodRef.update({
      price: -5.0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Valid partial update succeeds
  await assertSucceeds(
    prodRef.update({
      price: 13.49,
      locationName: "Central Farmers Stall · Row 2",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("OrderRecord: create starts CONFIRMED, supports status progression, locks at DELIVERED, blocks delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);
  const orderRef = aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1001");

  await assertSucceeds(
    orderRef.set({
      userId: ALICE_UID,
      orderNumber: "FC-1001",
      status: "CONFIRMED",
      deliveryAddress: "742 Evergreen Terrace",
      deliveryCity: "San Francisco",
      fulfillmentHub: "Downtown Fresh Hub",
      itemCount: 3,
      subtotal: 24.5,
      deliveryFee: 2.99,
      taxAmount: 2.02,
      totalCost: 29.51,
      itemsSummary: ["2x Hass Avocados ($4.49) · Westside Hub", "1x Pour-Over Kettle ($15.52) · Home Hub"],
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    orderRef.update({
      status: "DELIVERED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state lock: cannot update after DELIVERED
  await assertFails(
    orderRef.update({
      status: "CANCELLED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Orders cannot be deleted
  await assertFails(orderRef.delete());
});
