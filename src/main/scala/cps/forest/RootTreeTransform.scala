package cps.forest

import scala.quoted._

import cps._
import cps.misc._


trait RootTreeTransform[F[_], CT]:

  thisTransform: TreeTransformScope[F, CT] =>
  
  import qctx.tasty._

  def runRoot(term: qctx.tasty.Term, marker: TransformationContextMarker): CpsTree =
     if (cpsCtx.flags.debugLevel >= 15)
        cpsCtx.log(s"runRoot: term=$safeShow(term)")
     val r = term.tpe.widen match {
       case _ : MethodType =>
               //  in such case, we can't transform tree to expr
               //  without eta-expansion.  
               //    from other side - we don't want do eta-expand now, it can be performed early.
                runRootUneta(term, marker)
       case _ : PolyType =>
                runRootUneta(term, marker)
       case _ =>
                term match
                  case lambdaTerm@Lambda(params, body) => 
                                 // type of cps[x => y]  is  x=>F[y], not F[X=>Y]
                                 //  and if it violate CpsExpr contract (which require F[X=>Y]), let's
                                 //  work with lambda on the tree level.
                                 runLambda(lambdaTerm, params, body)
                  case applyTerm@Apply(fun,args)  =>
                            val tree = B2.inNestedContext(applyTerm, marker, scope =>
                               scope.runApply(applyTerm.asInstanceOf[scope.qctx.tasty.Term],
                                              fun.asInstanceOf[scope.qctx.tasty.Term],
                                              args.asInstanceOf[List[scope.qctx.tasty.Term]],
                                              Nil).asInstanceOf[CpsTree]
                            )
                            tree
                  case _ =>  // TODO: elimi
                    val expr = term.seal
                    val monad = cpsCtx.monad
                    expr match 
                      case '{ $e: $et } =>
                        val rCpsExpr = try {
                             Async.nestTransform(e, cpsCtx, marker)
                        } catch {
                             case e: Throwable =>
                                println(s"can't translate tree: $term" )
                                throw e;
                        }
                        val r = exprToTree(rCpsExpr, term)
                        if cpsCtx.flags.debugLevel >= 10 then
                           cpsCtx.log(s"runRoot: rCpsExpr=$rCpsExpr, async=${rCpsExpr.isAsync}")
                           if cpsCtx.flags.debugLevel >= 15 then
                             cpsCtx.log(s"runRoot: r=$r, async=${r.isAsync}, origin=$term")
                        r
                      case _ =>
                        throw MacroError("Can't determinate exact type for term", expr)
     }
     if (cpsCtx.flags.debugLevel >= 15)
        cpsCtx.log(s"runRoot result: $r")
     r


  def runRootUneta(term: qctx.tasty.Term, marker: TransformationContextMarker): CpsTree = {
     // TODO: change cpsCtx to show nesting
     if (cpsCtx.flags.debugLevel >= 15)
        cpsCtx.log(s"runRootUneta, term=$term")
     val monad = cpsCtx.monad
     val r = term match {
       case Select(qual, name) =>
           runRoot(qual, TransformationContextMarker.Select) match 
              case rq: AsyncCpsTree =>
                  val cTransformed = rq.transformed.asInstanceOf[qctx.tasty.Term]
                  CpsTree.impure(Select(cTransformed,term.symbol),term.tpe)
              case _: PureCpsTree =>
                  CpsTree.pure(term)
       case Ident(name) =>
             CpsTree.pure(term)
       case Apply(x, args) =>
             val thisScope = this
             val nestContext = cpsCtx.nestSame(marker)
             val nestScope = new TreeTransformScope[F,CT] {
                override val cpsCtx = nestContext
                override implicit val qctx = thisScope.qctx
                override val fType = thisScope.fType
                override val ctType = thisScope.ctType
             }
             nestScope.runApply(term.asInstanceOf[nestScope.qctx.tasty.Term],
                                x.asInstanceOf[nestScope.qctx.tasty.Term],
                                args.asInstanceOf[List[nestScope.qctx.tasty.Term]],
                                Nil).asInstanceOf[CpsTree]
       case _ =>
             throw MacroError(s"cps tree transform is not supported yet to ${term}",cpsCtx.patternCode)
     }
     if (cpsCtx.flags.debugLevel >= 15)
        cpsCtx.log(s"runRootUneta result: $r  (term=$term)")
     r
  }

  def exprToTree(expr: CpsExpr[F,_], e: Term): CpsTree =
     if (expr.isAsync)
         val transformed = expr.transformed.unseal
         AwaitSyncCpsTree(transformed, e.tpe)
     else
         PureCpsTree(e)

  object B2{

   def inNestedContext(term: Term, 
                      marker: TransformationContextMarker,
                      op: TreeTransformScope[F,?] => CpsTree): CpsTree =
        val nScope = if (false && term.isExpr) {
           term.seal match
             case '{ $e: $et} =>
                nestScope(e, marker)
             case _ =>
                throw MacroError("Can't determinate type for ${e.show}",posExprs(term))
        } else {
           nestScope(cpsCtx.patternCode, marker)
        }
        op(nScope)


   def nestScope[E:quoted.Type](e: Expr[E], marker: TransformationContextMarker): TreeTransformScope[F,E] =
       val et = summon[quoted.Type[E]]
       val nContext = cpsCtx.nest(e, et, marker)
       new TreeTransformScope[F,E] {
            override val cpsCtx = nContext
            override implicit val qctx = thisTransform.qctx
            override val fType = thisTransform.fType
            override val ctType = et
       }

  }



