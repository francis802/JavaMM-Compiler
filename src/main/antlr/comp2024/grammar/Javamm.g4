grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
SUB : '-' ;


CLASS : 'class' ;
INT : 'int' ;
DOUBLE : 'double' ;
FLOAT : 'float' ;
BOOLEAN : 'boolean' ;
STRING : 'String' ;
PUBLIC : 'public' ;
VOID : 'void' ;
STATIC : 'static' ;
RETURN : 'return' ;

INTEGER : [0] | ([1-9][0-9]*) ;
ID : [a-zA-Z_$]([a-zA-Z_$0-9])* ;

WS : [ \t\n\r\f]+ -> skip ;
SCOMMENT : '//' .*? '\n' -> skip;
MCOMMENT : '/*' .*? '*/' -> skip;


program
    : (importDecl)* classDecl EOF
    ;


importDecl
    : 'import' name+=ID ('.' name+=ID)* SEMI #ImportStmt
    ;

classDecl
    : CLASS classname=ID ('extends' superclass=ID)? LCURLY varDecl* methodDecl* RCURLY;

varDecl
    : type name=ID SEMI;

type locals[boolean isArray=false, boolean isVarArgs=false]
    : name=INT ('[' ']' {$isArray=true;} | '...' {$isArray=true; $isVarArgs=true;})?
    | name= DOUBLE
    | name= FLOAT
    | name= BOOLEAN
    | name= STRING
    | name = ID
    ;


methodDecl locals[boolean isPublic=false, boolean isMain=false]
    : (PUBLIC {$isPublic=true;})? type name=ID LPAREN param? RPAREN LCURLY varDecl* stmt* RCURLY
    | (PUBLIC {$isPublic=true;})? STATIC VOID name=ID LPAREN STRING '[' ']' arg=ID RPAREN LCURLY (varDecl)* (stmt)* RCURLY {$isMain=true;}
    ;


param
    : type name=ID (',' param)*
    ;


stmt
    : expr SEMI #ExprSmt //
    | LCURLY (stmt)* RCURLY #CurlyStmt //
    | RETURN expr SEMI #ReturnStmt //
    | ifExpr elseExpr #ConditionalStmt //
    | 'for' LPAREN stmt expr ';' expr RPAREN stmt #ForStmt //
    | 'while' LPAREN expr RPAREN stmt #WhileStmt //
    | expr EQUALS expr SEMI #AssignStmt //
    ;


ifExpr
    : 'if' LPAREN expr RPAREN stmt;

elseExpr
    : 'else' stmt;


expr
    : LPAREN expr RPAREN #Parenthesis //
    | expr '[' expr ']' #ArraySubs //
    | 'new' type '[' expr ']' #ArrayDeclaration //
    | 'new' name=ID LPAREN (expr (',' expr)*)? RPAREN #ObjectDeclaration //
    | expr '.' name=ID LPAREN (expr (',' expr)*)? RPAREN #FunctionCall //
    | expr op= (MUL | DIV) expr #BinaryExpr //
    | expr op= (ADD | SUB) expr #BinaryExpr //
    | expr op=('*=' | '/=' | '*=' | '-=') expr #BinaryExpr //
    | expr op=('<' | '>' | '<=' | '>=' | '==' | '!=') expr #BinaryExpr //
    | expr op='&&' expr #BinaryExpr //
    | expr op='||' expr #BinaryExpr //
    | value=INTEGER #IntegerLiteral //
    | value='true' #TrueLiteral //
    | value='false' #FalseLiteral //
    | name=ID #VarRefExpr //
    | value = 'this' #Object //
    | value = '!' expr #Negation //
    | expr '.' 'length' #Length //
    | '[' ( expr ( ',' expr )* )? ']' #DescribedArray
    ;



