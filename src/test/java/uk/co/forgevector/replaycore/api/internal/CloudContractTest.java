/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import uk.co.forgevector.replaycore.api.Fixtures;

/**
 * Pins the SDK's models to the cloud's JSON.
 *
 * <p>Each fixture under {@code contract/} carries every key the cloud's Go
 * serialiser emits for one response shape. For every key, at every depth the
 * models map, the test removes the key, maps the tree again and compares what the
 * models expose. A key that changes nothing is not surfaced by any getter, and the
 * test fails unless the key is listed in that shape's ignore set. When the cloud
 * adds a field and the fixture is refreshed, the new key fails here until the
 * client maps it or a reviewer decides to ignore it, with a reason.
 *
 * <p>Paths use {@code .} for an object member and {@code []} for the first element
 * of an array. An ignore entry that no longer names a real key, or names a key that
 * is now surfaced, also fails, so the sets cannot go stale.
 */
class CloudContractTest {

    private interface Mapper {
        Object map(Map<String, Object> tree);
    }

    private static final Set<String> NONE = Collections.<String>emptySet();

    private static Set<String> set(String... paths) {
        return new HashSet<String>(Arrays.asList(paths));
    }

    @Test
    void replay() {
        check("replay.json", new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toReplayMetadata(t);
            }
        }, NONE, NONE);
    }

    @Test
    void replayPage() {
        check("replay-page.json", new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toReplayPage(t);
            }
        }, NONE, NONE);
    }

    @Test
    void servers() {
        // installation, captureHealth and captureHealthReceivedAt are recorder diagnostics,
        // not part of the typed contract; read them with requestJson. pending lists
        // setups that have not connected yet and is returned by createServer's follow-up
        // reads, and expiredLoaderDownloads is only sent to a dashboard actor.
        check("servers.json", new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toServerInstances(t);
            }
        }, set("servers[].installation", "servers[].captureHealth", "servers[].captureHealthReceivedAt",
                "pending", "expiredLoaderDownloads"), NONE);
    }

    @Test
    void timelineMarker() {
        check("timeline-marker.json", new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toTimelineMarker(t);
            }
        }, NONE, NONE);
    }

    @Test
    void collection() {
        check("collection.json", collectionMapper(), NONE, set("metadata"));
    }

    @Test
    void collectionPage() {
        check("collection-page.json", new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toCollectionPage(t);
            }
        }, NONE, set("results[].metadata"));
    }

    @Test
    void collectionBulkLookup() {
        check("collection-bulk-lookup.json", new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toReplayCollections(t);
            }
        }, NONE, set("results[].metadata"));
    }

    @Test
    void collectionFacets() {
        check("collection-facets.json", new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toCatalogFacets(t);
            }
        }, NONE, NONE);
    }

    @Test
    void collectionMarkers() {
        check("collection-markers.json", new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toScopeMarkers(t);
            }
        }, NONE, NONE);
    }

    @Test
    void asset() {
        check("asset.json", new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toReplayAsset(t);
            }
        }, NONE, NONE);
    }

    @Test
    void playerReplays() {
        check("player-replays.json", new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toAssetPage(t);
            }
        }, NONE, NONE);
    }

    @Test
    void operation() {
        check("operation.json", new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toScopeOperation(t);
            }
        }, NONE, NONE);
    }

    @Test
    void scopeBatch() {
        check("scope-batch.json", new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toBatchResults(t);
            }
        }, NONE, NONE);
    }

    @Test
    void watchTicket() {
        check("watch-ticket.json", new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toWatchTicket(t);
            }
        }, NONE, NONE);
    }

    @Test
    void watchTicketPreparing() {
        check("watch-ticket-preparing.json", new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toWatchTicket(t);
            }
        }, NONE, NONE);
    }

    @Test
    void watchTicketRedeem() {
        check("watch-ticket-redeem.json", new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toWatchTicketRedemption(t);
            }
        }, NONE, NONE);
    }

    @Test
    void watchLink() {
        check("watch-link.json", new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toWatchLink(t);
            }
        }, NONE, NONE);
    }

    @Test
    void webhookDeliveryReplay() {
        check("webhook-delivery-replay.json", new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toWebhookDeliveryReplay(t);
            }
        }, NONE, NONE);
    }

    private static Mapper collectionMapper() {
        return new Mapper() {
            @Override
            public Object map(Map<String, Object> t) {
                return ModelMapper.toReplayCollection(t);
            }
        };
    }

    // ------------------------------------------------------------------ engine

    private static void check(String fixture, Mapper mapper, Set<String> ignored, Set<String> opaque) {
        Map<String, Object> tree = Fixtures.object(fixture);
        String baseline = dump(mapper.map(tree));

        List<String> paths = new ArrayList<String>();
        collect(tree, "", ignored, opaque, paths);

        List<String> unsurfaced = new ArrayList<String>();
        List<String> staleIgnores = new ArrayList<String>();
        for (String path : paths) {
            boolean surfaced = surfaced(fixture, mapper, baseline, path);
            if (ignored.contains(path)) {
                if (surfaced) {
                    staleIgnores.add(path);
                }
            } else if (!surfaced) {
                unsurfaced.add(path);
            }
        }
        for (String path : ignored) {
            assertTrue(paths.contains(path), fixture + ": ignore entry names no key in the fixture: " + path);
        }
        assertTrue(unsurfaced.isEmpty(),
                fixture + ": keys the client does not map (map them, or list them as ignored with a reason): " + unsurfaced);
        assertTrue(staleIgnores.isEmpty(),
                fixture + ": ignored keys the client now maps (remove them from the ignore set): " + staleIgnores);
        assertEquals(baseline, dump(mapper.map(Fixtures.object(fixture))), fixture + ": mapping is not deterministic");
    }

    private static boolean surfaced(String fixture, Mapper mapper, String baseline, String path) {
        Map<String, Object> copy = Fixtures.object(fixture);
        remove(copy, path);
        try {
            return !baseline.equals(dump(mapper.map(copy)));
        } catch (RuntimeException e) {
            // The mapper refused the tree without this key, so it certainly reads it.
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    private static void collect(Object node, String prefix, Set<String> ignored, Set<String> opaque,
                                List<String> out) {
        if (node instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) node).entrySet()) {
                String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                out.add(path);
                if (!ignored.contains(path) && !opaque.contains(path)) {
                    collect(entry.getValue(), path, ignored, opaque, out);
                }
            }
        } else if (node instanceof List && !((List<?>) node).isEmpty()) {
            collect(((List<?>) node).get(0), prefix + "[]", ignored, opaque, out);
        }
    }

    @SuppressWarnings("unchecked")
    private static void remove(Object root, String path) {
        Object node = root;
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            boolean array = part.endsWith("[]");
            String key = array ? part.substring(0, part.length() - 2) : part;
            Map<String, Object> map = (Map<String, Object>) node;
            if (i == parts.length - 1 && !array) {
                map.remove(key);
                return;
            }
            Object child = map.get(key);
            node = array ? ((List<Object>) child).get(0) : child;
            if (i == parts.length - 1) {
                ((List<Object>) child).remove(0);
                return;
            }
        }
    }

    /** Renders everything the public getters of a model expose, so two trees compare by content. */
    private static String dump(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Optional) {
            Optional<?> o = (Optional<?>) value;
            return o.isPresent() ? "Optional[" + dump(o.get()) + "]" : "Optional.empty";
        }
        if (value instanceof OptionalInt) {
            OptionalInt o = (OptionalInt) value;
            return o.isPresent() ? "OptionalInt[" + o.getAsInt() + "]" : "OptionalInt.empty";
        }
        if (value instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            for (Object element : (List<?>) value) {
                sb.append(dump(element)).append(',');
            }
            return sb.append(']').toString();
        }
        if (value instanceof Map) {
            return dump(new ArrayList<Object>(new TreeMap<Object, Object>((Map<?, ?>) value).entrySet()));
        }
        if (value instanceof Map.Entry) {
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) value;
            return e.getKey() + "=" + dump(e.getValue());
        }
        if (value.getClass().getName().startsWith("uk.co.forgevector.replaycore.api.model.")
                && !value.getClass().isEnum()) {
            Map<String, String> parts = new LinkedHashMap<String, String>();
            List<Method> methods = new ArrayList<Method>(Arrays.asList(value.getClass().getMethods()));
            Collections.sort(methods, new java.util.Comparator<Method>() {
                @Override
                public int compare(Method a, Method b) {
                    return a.getName().compareTo(b.getName());
                }
            });
            for (Method m : methods) {
                String name = m.getName();
                boolean getter = name.startsWith("get") || name.startsWith("is") || name.startsWith("has");
                if (!getter || m.getParameterTypes().length != 0 || name.equals("getClass")
                        || java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                try {
                    parts.put(name, dump(m.invoke(value)));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (InvocationTargetException e) {
                    throw new IllegalStateException(e.getCause());
                }
            }
            return value.getClass().getSimpleName() + parts;
        }
        return String.valueOf(value);
    }
}
