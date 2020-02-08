package nu.mine.mosher.neo4j.demo;

import org.neo4j.ogm.annotation.*;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

import java.io.Serializable;
import java.util.UUID;

@RelationshipEntity(type=R.TYPE)
public class R implements Serializable, Comparable<R> {
    public static final String TYPE = "R";

    private static final long serialVersionUID = 1L;
    @Id @GeneratedValue Long id;
    @Version Long version;
    @Convert(UuidStringConverter.class) @Index(unique=true) @Id UUID uuid;

    @Property String name;

    @StartNode X x;
    @EndNode Y y;

    R() {
    }

    R(final String name, final X x, final Y y) {
        this.name = name;
        this.uuid = UUID.randomUUID();
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return String.format("%s{id=%d,version=%d,uuid=%s,name=%s}", TYPE, id, version, uuid, name);
    }

    @Override
    public int compareTo(R that) {
        return this.name.compareTo(that.name);
    }
}
