package io.github.greyplane.dbcrul.infrastructure

/** Import the members of this object when defining SQL queries using doobie.
  */
object Doobie
    extends doobie.Aliases
    with doobie.hi.Modules
    with doobie.free.Modules
    with doobie.free.Types
    with doobie.postgres.Instances
    with doobie.util.meta.LegacyInstantMetaInstance
    with doobie.free.Instances
    with doobie.syntax.AllSyntax
