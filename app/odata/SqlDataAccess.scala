package odata

import java.sql.{Connection, PreparedStatement}

import com.zaxxer.hikari.HikariDataSource

import scala.util.Try

object SqlDataAccess {

  private def tryEither[A](inner: => A): Either[Throwable, A] = Try(inner).toEither

  def usingConnection[A](code: Connection => Either[Throwable, A])(implicit dataSource: HikariDataSource): Either[Throwable, A] = {
    val conn   = tryEither { dataSource.getConnection() }
    val result = conn.flatMap(c => code(c))
    conn.map(c => c.close())
    result
  }

  def withStatement[A](sql: String)(code: PreparedStatement => A)(implicit connection: Connection): Either[Throwable, A] = {
    val stm    = tryEither { connection.prepareStatement(sql) }
    val result = stm.map(s => code(s))
    stm.map(s => s.close())
    result
  }
}