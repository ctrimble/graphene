# Graphene

A project for running large scale graph computations with OpenCL.  This project is a toy.  You have been warned.

## What Users Need To Provide

### The Schema

I am not sure how this will be specified, but it needs to define something that can be translated into
a C99 struct and be used to generate java code for building the structures.

### The Functions

The transition functions will operate in bulk, so that C99 can be used in cases where the transitions are programatic.  In other cases, the transitions could come from a file, S3, etc.

```
  @InEdgeFunction
  public <V extends Vertex> Function<V[], Edge<V, V>[]> inEdges();
  @OutEdgeFunction
  public <V extends Vertex> Function<V[], Edge<V, V>[]> outEdges();
```

### The Sequence

The sequence will allow for scanning over the entire state space.  It will need to support ranges, so that the space can be subdivided.

```
  @VertexSpace
  public Provider<Collection<V>> vertexSpace();
```

This signature probably needs to be improved, since we want to be doing things in bulk whenever possible.  Perhaps something like this instead:

```
  @VertexSpace
  public Provider<Sequence<V[]>> vertexSpace(int chunkSize)
```

### Data Model

The data model is split into 3 sets of tables:

#### Vertex Data

Vertex data is stored as ids and current data at those vertices.

<table>
  <tr><td>ID</td><td>data</td></tr>
</table>

#### Internal Edge Data

<table>
  <tr><td>in ID</td><td>out ID</td><td>out data</td>
</table>

#### External Edge

<table>
  <tr><td>in ID</td><td>out partition ID</td><td>out ID</td><td>out data</td></tr>
</table>

### Computation

Computation will happen in phases:

#### Phase 1: Expansion

The first step will be initializing the start state for the graph.  This phase will need to:

- Identify all of the external partitions that lead to the current partition.
- Download the external partitions and build their external edge tables.
- Generate the vertex table and initialize it.
- Generate the internal edge table and intialize it.

The tables created here are not in one location, but broken up among the members of the cluster.  Node failures will result in data loss and a restart of the computation.

#### Phase 2: Compute

Vertex data is transmitted around the cluster, 