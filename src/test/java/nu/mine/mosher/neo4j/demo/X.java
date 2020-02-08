package nu.mine.mosher.neo4j.demo;

import org.neo4j.ogm.annotation.*;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.typeconversion.UuidStringConverter;

import java.io.Serializable;
import java.util.*;

@NodeEntity(label=X.TYPE)
public class X implements Serializable {
    public static final String TYPE = "X";
    private static final long serialVersionUID = 1L;
    @Id @GeneratedValue Long id;
    @Version Long version;
    @Convert(UuidStringConverter.class) @Index(unique=true) @Id UUID uuid;

    @Property String name;

    @Relationship(type=R.TYPE)
    TreeSet<R> refs = new TreeSet<>();

    X() {
    }

    X(final String name) {
        this.name = name;
        this.uuid = UUID.randomUUID();
    }

    @Override
    public String toString() {
        return String.format("%s{id=%d,version=%d,uuid=%s,name=%s,#refs=%s}", TYPE, id, version, uuid, name, refs.size());
    }
}
