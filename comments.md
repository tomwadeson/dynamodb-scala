Comments on dynamodb-scala
==========================

## General impressions

This is, on the whole, good quality code.  It's a library that I'd be happy to use and to contribute to.

I like:

 - The testing strategy: a good mix of property based testing of `Encoder` & `Decoder` invariants, and integration testing against the DB.  There's 85% coverage (by lines) in the top most `com.onzo.dyanmodb` package.  This reduces (but does not eliminate) the risk of introducing regressions as part of any refactoring work that's carried out.
 - The generic programming techniques employed: libraries which provide such APIs are a pleasure to work with as an application developer, where I don't have to worry about the complexity under the covers and can instead take the productivity boost of not having to write so much boilerplate.  That said, I suspect there's room for improvement on this front -- see the final section.
 - Its focus and its size.  It's small and it's built for one thing: mapping case classes to tables in DynamoDB.

It uses some advanced techniques which could make it difficult to reason about and maintain for someone who doesn't have much experience with these concepts.  Here are a few of them:

 - Functors
 - Contravariant Functors
 - Monads
 - Type classes and implicits
 - Generic programming: `HList`s
 - Property-based testing

That said, my preference would never be to prescribe the use of specific techniques, approaches or language features.  Instead, judgement should be used to decide whether or not such powerful tools are appropriate and proportional to the problem and -- if they are -- knowledge needs to be transferred throughout the team with tools like pair-programming, code review and show and tells.

## Approachability to Scala newcomers

A couple of my commits were made with the design intent of making the library more approachable to Scala newcomers.  Specifically,

 - Commit 56dcc64 slaps a meaningful name on the otherwise idomatic Scala convention of naming a type class creation function `instance`.  Once a developer is exposed to the type class pattern and becomes familiar with its encoding in Scala, using `instance` is very natural.  Until then, being explicit with the intent is preferable (i.e. `createEncoder`, `createDecoder`.)
 - Commit db67a25 renames `apply` on `Encoder#apply` and `Decoder#apply` to `Encoder#encode` and `Decoder#decode`, respectively.  The trade-off here is that while you're no longer able to apply instances of `Decoder` and `Encoder` directly to parameters as if they were functions (as in `val encoder: Encoder[Int] = ...; encoder(10)`), there are fewer concepts to keep in mind when reading the code.

## Increasing genericity 

shapeless provides `Generic` to map between ADTs and their generic representations (`HList`s and `Coproduct`s).  With this tool, it would be possible to increase the genericity of this library such that users would not be required to write as much boilerplate for simple use-cases.  For example, a generic serialiser could be created that would, by convention, take the first field in a case class to be the primary key and map fields by name (with the use of `LabelledGeneric`) to table columns.

