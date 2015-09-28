package ca.uwaterloo.flix.runtime

import ca.uwaterloo.flix.lang.ast.TypedAst.{Expression, Literal, Pattern, Type, FormalArg, Root}
import ca.uwaterloo.flix.lang.ast.{ParsedAst, BinaryOperator, UnaryOperator}

object Interpreter {
  type Env = Map[ParsedAst.Ident, Value]

  def eval(expr: Expression, root: Root, env: Env = Map()): Value = {
    expr match {
      case Expression.Lit(literal, _) => evalLit(literal)
      case Expression.Var(ident, _) =>
        assert(env.contains(ident), s"Expected variable ${ident.name} to be bound.")
        env(ident)
      case Expression.Ref(name, _) =>
        assert(root.constants.contains(name), s"Expected constant ${name.parts.mkString(".")} to be defined.")
        eval(root.constants(name).exp, root, env)
      case Expression.Lambda(formals, _, body, _) => Value.Closure(formals, body, env)
      case Expression.Apply(exp, args, _) => eval(exp, root, env) match {
          case Value.Closure(formals, body, closureEnv) =>
            val evalArgs = args.map(x => eval(x, root, env))
            val newEnv = closureEnv ++ formals.map(_.ident).zip(evalArgs).toMap
            eval(body, root, newEnv)
          case _ => assert(false, "Expected a function."); Value.Unit
        }
      case Expression.Unary(op, exp, _) => evalUnary(op, eval(exp, root, env))
      case Expression.Binary(op, exp1, exp2, _) => evalBinary(op, eval(exp1, root, env), eval(exp2, root, env))
      case Expression.IfThenElse(exp1, exp2, exp3, tpe) =>
        val cond = eval(exp1, root, env).toBool
        if (cond) eval(exp2, root, env) else eval(exp3, root, env)
      case Expression.Let(ident, value, body, tpe) =>
        // TODO: Right now Let only supports a single binding. Does it make sense to allow a list of bindings?
        val func = Expression.Lambda(List(FormalArg(ident, value.tpe)), tpe,
          body, Type.Function(List(value.tpe), tpe))
        val desugared = Expression.Apply(func, List(value), tpe)
        eval(desugared, root, env)
      case Expression.Match(exp, rules, _) =>
        val value = eval(exp, root, env)
        matchRule(rules, value) match {
          case Some((matchExp, matchEnv)) => eval(matchExp, root, env ++ matchEnv)
          case None => throw new RuntimeException(s"Unmatched value $value.")
        }
      case Expression.Tag(name, ident, exp, _) => Value.Tag(name, ident.name, eval(exp, root, env))
      case Expression.Tuple(elms, _) => Value.Tuple(elms.map(e => eval(e, root, env)))
      case Expression.Error(location, tpe) => location.path match {
        case Some(path) => throw new RuntimeException(s"Error at $path:${location.line}:${location.column}.")
        case None => throw new RuntimeException("Error at unknown location.")
      }
    }
  }

  private def evalLit(lit: Literal): Value = lit match {
    case Literal.Unit => Value.Unit
    case Literal.Bool(b) => Value.Bool(b)
    case Literal.Int(i) => Value.Int(i)
    case Literal.Str(s) => Value.Str(s)
    case Literal.Tag(name, ident, innerLit, _) => Value.Tag(name, ident.name, evalLit(innerLit))
    case Literal.Tuple(elms, _) => Value.Tuple(elms.map(evalLit))
  }

  private def evalUnary(op: UnaryOperator, v: Value): Value = op match {
    case UnaryOperator.Not => Value.Bool(!v.toBool)
    case UnaryOperator.UnaryPlus => Value.Int(+v.toInt)
    case UnaryOperator.UnaryMinus => Value.Int(-v.toInt)
  }

  private def evalBinary(op: BinaryOperator, v1: Value, v2: Value): Value = op match {
    case BinaryOperator.Plus => Value.Int(v1.toInt + v2.toInt)
    case BinaryOperator.Minus => Value.Int(v1.toInt - v2.toInt)
    case BinaryOperator.Times => Value.Int(v1.toInt * v2.toInt)
    case BinaryOperator.Divide => Value.Int(v1.toInt / v2.toInt)
    case BinaryOperator.Modulo => Value.Int(v1.toInt % v2.toInt) // TODO: Document semantics of modulo on negative operands
    case BinaryOperator.Less => Value.Bool(v1.toInt < v2.toInt)
    case BinaryOperator.LessEqual => Value.Bool(v1.toInt <= v2.toInt)
    case BinaryOperator.Greater => Value.Bool(v1.toInt > v2.toInt)
    case BinaryOperator.GreaterEqual => Value.Bool(v1.toInt >= v2.toInt)
    case BinaryOperator.Equal => Value.Bool(v1 == v2)
    case BinaryOperator.NotEqual => Value.Bool(v1 != v2)
    case BinaryOperator.And => Value.Bool(v1.toBool && v2.toBool)
    case BinaryOperator.Or => Value.Bool(v1.toBool || v2.toBool)
    case BinaryOperator.Minimum => Value.Int(math.min(v1.toInt, v2.toInt))
    case BinaryOperator.Maximum => Value.Int(math.max(v1.toInt, v2.toInt))
    case BinaryOperator.Union | BinaryOperator.Subset =>
      assert(false, "Can't have union or subset operators."); Value.Unit
  }

  private def matchRule(rules: List[(Pattern, Expression)], value: Value): Option[(Expression, Env)] = rules match {
    case (pattern, exp) :: rest => unify(pattern, value) match {
      case Some(env) => Some((exp, env))
      case None => matchRule(rest, value)
    }
    case Nil => None
  }

  private def unify(pattern: Pattern, value: Value): Option[Env] = (pattern, value) match {
    case (Pattern.Wildcard(_), _) => Some(Map())
    case (Pattern.Var(ident, _), _) => Some(Map(ident -> value))
    case (Pattern.Lit(lit, _), _) if evalLit(lit) == value => Some(Map())
    case (Pattern.Tag(name1, ident1, innerPat, _), Value.Tag(name2, ident2, innerVal))
      if name1 == name2 && ident1.name == ident2 => unify(innerPat, innerVal)
    case (Pattern.Tuple(pats, _), Value.Tuple(vals)) =>
      val envs = pats.zip(vals).map { case (p, v) => unify(p, v) }.collect { case Some(e) => e }
      if (pats.size == envs.size)
        Some(envs.foldLeft(Map[ParsedAst.Ident, Value]()) { case (acc, newEnv) => acc ++ newEnv })
      else None
    case _ => None
  }
}
