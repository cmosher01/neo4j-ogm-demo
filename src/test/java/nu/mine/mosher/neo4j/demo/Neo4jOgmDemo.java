package nu.mine.mosher.neo4j.demo;

import org.junit.jupiter.api.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver;
import org.neo4j.ogm.session.*;
import org.slf4j.*;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class Neo4jOgmDemo {
    private static final Logger LOG = LoggerFactory.getLogger(Neo4jOgmDemo.class);



    static SessionFactory factorySession;

    @BeforeAll
    static void startDatabase() {
        final GraphDatabaseService db = new GraphDatabaseFactory().
            newEmbeddedDatabaseBuilder(new File("database")).
            newGraphDatabase();

        final Configuration configuration = new Configuration.Builder().build();
        final EmbeddedDriver driver = new EmbeddedDriver(db, configuration);

        factorySession = new SessionFactory(driver, Neo4jOgmDemo.class.getPackage().getName());
    }

    @AfterAll
    static void stopDatabase() {
        factorySession.close();
    }



    Session session;

    @BeforeEach
    void startSession() {
        this.session = factorySession.openSession();
        this.session.clear();
    }

    @AfterEach
    void clearSession() {
        this.session.clear();
    }



    @Test
    void demo() {
        // many to many relationship
        //     (X) >0---[R]---0< (Y)

        // create and save
        UUID uuidX;
        UUID uuidY;
        UUID uuidR;
        {
            X x = new X("x");
            uuidX = x.uuid;

            Y y = new Y("y");
            uuidY = y.uuid;

            // this creates the relationship and populates its start and end nodes
            // but doesn't set the back-pointers in x or y.
            R r = new R("r", x, y);
            uuidR = r.uuid;

            LOG.debug("created x: {}", x);
            LOG.debug("created r: {}", r);
            LOG.debug("created y: {}", y);

            // saving r also saves x and y
            session.save(r);

            // the IDs in all objects get updated
            // (The "id" and "version" properties should be treated as
            // being for internal use by OGM.)
            assertNotNull(x.id);
            assertNotNull(y.id);
            assertNotNull(r.id);

            // The back-pointers in x and y are not updated here in java,
            // but the nodes and relationships are written correctly in the database

            LOG.debug("saved x: {}", x);
            LOG.debug("saved r: {}", r);
            LOG.debug("saved y: {}", y);

            // In general, after saving the (java) model, don't use it anymore.
            // Instead, re-read objects from the database and update them that way.

            session.clear();
        }

        // reload and check
        {
            // reload x, specifying depth as -1 (unlimited), which
            // loads all 3 entities: x, r, and y
            X x = session.load(X.class, uuidX, -1);
            assertNotNull(x);

            // All pointers and back-pointers are fully populated

            LOG.debug("reloaded x: {}", x);
            assertNotNull(x.refs);
            assertEquals(1, x.refs.size());

            R r = x.refs.first();
            LOG.debug("reloaded r: {}", r);
            assertNotNull(r.x);
            assertSame(x, r.x);

            assertNotNull(r.y);
            Y y = r.y;
            LOG.debug("reloaded y: {}", y);
            assertNotNull(y.refs);
            assertEquals(1, y.refs.size());
            assertSame(r, y.refs.first());

            session.clear();
        }

        // modify a node's properties
        {
            X x = session.load(X.class, uuidX, 0);
            assertNotNull(x);
            LOG.debug("reloaded x: {}", x);
            // change the "name" property of x, and re-save
            x.name = "new-x";
            session.save(x);
            LOG.debug("saved changed x: {}", x);
            session.clear();

            // reload and check
            X x2 = session.load(X.class, uuidX, -1);
            assertNotNull(x2);
            LOG.debug("reloaded changed x: {}", x2);
            assertNotSame(x, x2);
            assertEquals("new-x", x2.name);
            assertEquals(x.id, x2.id);
            session.clear();
        }

        // modify a relationship's properties
        {
            R r = session.load(R.class, uuidR);
            assertNotNull(r);
            LOG.debug("reloaded r: {}", r);
            // change the "name" property of r, and re-save
            r.name = "new-r";
            session.save(r);
            LOG.debug("saved changed r: {}", r);
            session.clear();

            // reload and check
            R r2 = session.load(R.class, uuidR);
            assertNotNull(r2);
            LOG.debug("reloaded changed r: {}", r2);
            assertNotSame(r, r2);
            assertEquals("new-r", r2.name);
            assertEquals(r.id, r2.id);
            session.clear();
        }

        // delete a relationship
        {
            R r = session.load(R.class, uuidR);
            assertNotNull(r);
            LOG.debug("reloaded r: {}", r);
            session.delete(r);
            session.clear();

            // reload and check

            // the relationship references on x and y are removed
            X x = session.load(X.class, uuidX, -1);
            assertNotNull(x);
            LOG.debug("reloaded x: {}", x);
            assertTrue(x.refs.isEmpty());
            Y y = session.load(Y.class, uuidY, -1);
            assertNotNull(y);
            LOG.debug("reloaded y: {}", y);
            assertTrue(y.refs.isEmpty());

            // r does not exist
            R r2 = session.load(R.class, uuidR);
            assertNull(r2);
            session.clear();
        }

        UUID uuidNewR;
        // add a new relationship between two existing nodes
        {
            X x = session.load(X.class, uuidX, -1);
            assertNotNull(x);
            LOG.debug("reloaded x: {}", x);
            Y y = session.load(Y.class, uuidY, -1);
            assertNotNull(y);
            LOG.debug("reloaded y: {}", y);

            R r = new R("other-r", x, y);
            uuidNewR = r.uuid;
            session.save(r);
            session.clear();

            // reload and check
            X x2 = session.load(X.class, uuidX, -1);
            assertNotNull(x2);
            LOG.debug("reloaded x: {}", x2);
            assertNotSame(x, x2);
            assertNotNull(x2.refs);
            assertEquals(1, x2.refs.size());
            R r2 = x2.refs.first();
            LOG.debug("reloaded r: {}", r2);
            assertNotSame(r, r2);
            assertNotNull(r2.x);
            assertSame(x2, r2.x);
            assertEquals("other-r", r2.name);

            assertNotNull(r2.y);
            Y y2 = r2.y;
            assertNotSame(y, y2);
            LOG.debug("reloaded y: {}", y2);
            assertNotNull(y2.refs);
            assertEquals(1, y2.refs.size());
            assertSame(r2, y2.refs.first());
            session.clear();
        }

        // change one end of an existing relationship
        // change   (x:X)--[r:R]->(y:Y)   to   (x:X)--[r':R]->(y2:Y) and (y:Y)
        // If we were to delete r and recreate it as r', we would need to copy
        // all properties from r to r'. However, if we can reuse r (the java object),
        // and just reattach one end, we won't need to copy any properties.
        {
            R r = session.load(R.class, uuidNewR);
            assertNotNull(r);
            // Delete r from the database and the session.
            // This will also unhook x and y, in the database:
            //     (x:x)  (y:Y)
            session.delete(r);
            // We want to reuse the r java object locally, so we
            // make it look like a new object r' by removing its ID and version.
            // It will still have all its other properties, including (x:X) as its
            // StartNode (locally, even though it has been deleted in the database)
            r.id = null;
            r.version = null;

            // next we make a (y2:Y) and hook it up as the new EndNode of [r':R]
            Y y2 = new Y("y2");
            r.y = y2;
            LOG.debug("modified, to be saved r: {}", r);
            // this r will be treated as a new [:R] relationship by OGM when it saves:
            //     (x:X)--[r':R]->(y2:Y)
            session.save(r);
            // So now the database has these:
            //     (x:X)--[r':R]->(y2:Y) and (y:Y)
            session.clear();



            // reload, and check
            X x2 = session.load(X.class, uuidX, -1);
            assertNotNull(x2);
            LOG.debug("reloaded x: {}", x2);
            R r2 = x2.refs.first();
            LOG.debug("reloaded r: {}", r2);
            assertNotSame(r, r2);
            assertEquals(r.uuid, r2.uuid);
            assertEquals("other-r", r2.name);
            Y yp2 = r2.y;
            LOG.debug("reloaded y2: {}", yp2);
            assertNotSame(y2, yp2);
            assertEquals("y2", yp2.name);
            assertEquals(y2.uuid, yp2.uuid);
            assertEquals(1, yp2.refs.size());
            assertSame(r2, yp2.refs.first());

            // original y is now unhooked
            Y yold = session.load(Y.class, uuidY);
            assertNotNull(yold);
            LOG.debug("reloaded old y: {}", yold);
            assertEquals("y", yold.name);
            assertTrue(yold.refs.isEmpty());

            session.clear();
        }
    }
}
