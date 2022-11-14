package odata

import org.apache.olingo.commons.api.edm.{EdmEnumType, EdmPrimitiveTypeKind, EdmType}
import org.apache.olingo.commons.api.http.HttpStatusCode
import org.apache.olingo.server.api.ODataApplicationException
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty
import org.apache.olingo.server.api.uri.queryoption.expression.{
  BinaryOperatorKind,
  Expression,
  ExpressionVisitor,
  Literal,
  Member,
  MethodKind,
  UnaryOperatorKind
}

import java.util.Locale
import scala.jdk.CollectionConverters._

class FilterExpressionRedshiftVisitor extends ExpressionVisitor[String] {

  //scalastyle:off cyclomatic.complexity
  override def visitBinaryOperator(operator: BinaryOperatorKind, left: String, right: String): String = {
    //Implementation could be type dependant (think DateTime) so, better implementation would search for field definitions or something
    operator match {
      case BinaryOperatorKind.EQ if right == "null" => s"$left IS NULL"
      case BinaryOperatorKind.EQ if left == "null"  => s"$right IS NULL"
      case BinaryOperatorKind.EQ                    => s"$left = $right"
      case BinaryOperatorKind.NE if right == "null" => s"$left IS NOT NULL"
      case BinaryOperatorKind.NE if left == "null"  => s"$right IS NOT NULL"
      case BinaryOperatorKind.NE                    => s"$left <> $right"
      case BinaryOperatorKind.GT                    => s"$left > $right"
      case BinaryOperatorKind.LT                    => s"$left < $right"
      case BinaryOperatorKind.GE                    => s"$left >= $right"
      case BinaryOperatorKind.LE                    => s"$left <= $right"
      case BinaryOperatorKind.ADD                   => s"$left + $right"
      case BinaryOperatorKind.SUB                   => s"$left - $right"
      case BinaryOperatorKind.MUL                   => s"$left * $right"
      case BinaryOperatorKind.DIV                   => s"$left / $right"
      case BinaryOperatorKind.MOD                   => s"$left % $right"
      case BinaryOperatorKind.AND                   => s"$left AND $right"
      case BinaryOperatorKind.OR                    => s"$left OR $right"
      case other =>
        throw new ODataApplicationException(
          s"Binary operator: ${other} is not implemented.",
          HttpStatusCode.NOT_IMPLEMENTED.getStatusCode,
          Locale.ENGLISH
        )
    }
  }

  override def visitUnaryOperator(operator: UnaryOperatorKind, operand: String): String = {
    operator match {
      case UnaryOperatorKind.NOT   => s"NOT ${operand}"
      case UnaryOperatorKind.MINUS => s"-${operand}"
      case other =>
        throw new ODataApplicationException(
          s"Unary operator: ${other} is not implemented.",
          HttpStatusCode.NOT_IMPLEMENTED.getStatusCode,
          Locale.ENGLISH
        )
    }
  }

  override def visitMethodCall(methodCall: MethodKind, parameters: java.util.List[String]): String = {
    //TODO: Support common functions??
    //http://docs.oasis-open.org/odata/odata/v4.0/errata03/os/complete/part2-url-conventions/odata-v4.0-errata03-os-part2-url-conventions-complete.html#_Toc453752358

    val params = parameters.asScala

    methodCall match {
      case MethodKind.CONTAINS if params.length == 2 =>
        s"${params(0)} LIKE '%${params(1).replace("'", "")}%'"
      case _ =>
        throw new ODataApplicationException(
          s"Methods are currently not supported.",
          HttpStatusCode.NOT_IMPLEMENTED.getStatusCode,
          Locale.ENGLISH
        )
    }
  }

  override def visitLambdaExpression(lambdaFunction: String, lambdaVariable: String, expression: Expression): String = {
    throw new ODataApplicationException(
      s"Lambda expressions are currently not supported.",
      HttpStatusCode.NOT_IMPLEMENTED.getStatusCode,
      Locale.ENGLISH
    )
  }

  override def visitLiteral(literal: Literal): String = {
    //TODO: Check the type
    if (literal.getType == EdmPrimitiveTypeKind.String)
      s"'${literal.getText}'"
    else literal.getText
  }

  override def visitMember(member: Member): String = {
    val resourceParts = member.getResourcePath.getUriResourceParts.asScala

    if (resourceParts.size == 1 && resourceParts(0).isInstanceOf[UriResourcePrimitiveProperty]) {
      val uriResourceProperty = resourceParts(0).asInstanceOf[UriResourcePrimitiveProperty]
      uriResourceProperty.getProperty.getName
    } else {
      throw new ODataApplicationException(
        "Only primitive properties are implemented in filter expressions",
        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode,
        Locale.ENGLISH
      )
    }
  }

  override def visitAlias(aliasName: String): String =
    throw new ODataApplicationException("Aliasing is not implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH)

  override def visitTypeLiteral(`type`: EdmType): String =
    throw new ODataApplicationException(
      s"Type literals are not implemented: ${`type`}",
      HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
      Locale.ENGLISH
    )

  override def visitLambdaReference(variableName: String): String =
    throw new ODataApplicationException(
      s"Lambda references are not implemented: ${variableName}",
      HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
      Locale.ENGLISH
    )

  override def visitEnum(`type`: EdmEnumType, enumValues: java.util.List[String]): String =
    throw new ODataApplicationException(
      "Enums are not implemented: ${`type`}",
      HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
      Locale.ENGLISH
    )

  override def visitBinaryOperator(operator: BinaryOperatorKind, left: String, right: java.util.List[String]): String = {

//    operator match {
//      case BinaryOperatorKind.ADD
//    }
//
    throw new ODataApplicationException(
      s"Binary operators where right i a list are not implemented. Left: ${left}, Right: ${right}, operator: ${operator}",
      HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
      Locale.ENGLISH
    )
  }
}
