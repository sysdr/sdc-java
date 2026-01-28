package com.example.coordinator.service;

import com.example.coordinator.grammar.LogQueryLexer;
import com.example.coordinator.grammar.LogQueryParser;
import com.example.coordinator.grammar.LogQueryBaseVisitor;
import com.example.coordinator.model.QueryPlan;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class QueryParsingService {
    
    public QueryPlan parseQuery(String queryString) {
        log.info("Parsing query: {}", queryString);
        
        try {
            LogQueryLexer lexer = new LogQueryLexer(CharStreams.fromString(queryString));
            LogQueryParser parser = new LogQueryParser(new CommonTokenStream(lexer));
            
            QueryPlanBuilder visitor = new QueryPlanBuilder();
            QueryPlan plan = visitor.visit(parser.query());
            plan.setOriginalQuery(queryString);
            
            log.info("Successfully parsed query into plan: {}", plan);
            return plan;
            
        } catch (Exception e) {
            log.error("Failed to parse query: {}", queryString, e);
            throw new IllegalArgumentException("Invalid query syntax: " + e.getMessage());
        }
    }
    
    private static class QueryPlanBuilder extends LogQueryBaseVisitor<QueryPlan> {
        private QueryPlan.QueryPlanBuilder planBuilder = QueryPlan.builder();
        
        @Override
        public QueryPlan visitQuery(LogQueryParser.QueryContext ctx) {
            visit(ctx.selectClause());
            if (ctx.whereClause() != null) visit(ctx.whereClause());
            if (ctx.groupByClause() != null) visit(ctx.groupByClause());
            if (ctx.orderByClause() != null) visit(ctx.orderByClause());
            if (ctx.limitClause() != null) visit(ctx.limitClause());
            return planBuilder.build();
        }
        
        @Override
        public QueryPlan visitSelectClause(LogQueryParser.SelectClauseContext ctx) {
            boolean selectAll = ctx.fields().STAR() != null;
            List<String> fields = new ArrayList<>();
            
            if (!selectAll && ctx.fields().fieldList() != null) {
                ctx.fields().fieldList().IDENTIFIER().forEach(id -> 
                    fields.add(id.getText())
                );
            }
            
            planBuilder.selectClause(QueryPlan.SelectClause.builder()
                .selectAll(selectAll)
                .fields(fields)
                .build());
            
            return null;
        }
        
        @Override
        public QueryPlan visitWhereClause(LogQueryParser.WhereClauseContext ctx) {
            List<QueryPlan.Predicate> predicates = extractPredicates(ctx.condition());
            planBuilder.whereClause(QueryPlan.WhereClause.builder()
                .predicates(predicates)
                .build());
            return null;
        }
        
        @Override
        public QueryPlan visitLimitClause(LogQueryParser.LimitClauseContext ctx) {
            int limit = Integer.parseInt(ctx.NUMBER().getText());
            planBuilder.limitClause(QueryPlan.LimitClause.builder()
                .limit(limit)
                .build());
            return null;
        }
        
        private List<QueryPlan.Predicate> extractPredicates(LogQueryParser.ConditionContext ctx) {
            List<QueryPlan.Predicate> predicates = new ArrayList<>();
            
            if (ctx instanceof LogQueryParser.SimpleConditionContext) {
                LogQueryParser.SimpleConditionContext simple = (LogQueryParser.SimpleConditionContext) ctx;
                predicates.add(QueryPlan.Predicate.builder()
                    .field(simple.IDENTIFIER().getText())
                    .operator(simple.op().getText())
                    .value(extractValue(simple.value()))
                    .build());
            } else if (ctx instanceof LogQueryParser.AndConditionContext) {
                LogQueryParser.AndConditionContext and = (LogQueryParser.AndConditionContext) ctx;
                predicates.addAll(extractPredicates(and.condition(0)));
                predicates.addAll(extractPredicates(and.condition(1)));
            }
            
            return predicates;
        }
        
        private Object extractValue(LogQueryParser.ValueContext ctx) {
            if (ctx.STRING() != null) {
                String str = ctx.STRING().getText();
                return str.substring(1, str.length() - 1);
            } else if (ctx.NUMBER() != null) {
                return Double.parseDouble(ctx.NUMBER().getText());
            } else if (ctx.BOOLEAN() != null) {
                return Boolean.parseBoolean(ctx.BOOLEAN().getText());
            }
            return null;
        }
    }
}
