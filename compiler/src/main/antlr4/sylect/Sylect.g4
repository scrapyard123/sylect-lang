// SPDX-License-Identifier: MIT

grammar Sylect;

// GENERAL PROGRAM STRUCTURE
program: importSection? classDefinition (fieldDefinition | methodDefinition)*;

importSection: 'import' '{' IDENTIFIER+ '}';

classDefinition:
    ('interface' | 'class') IDENTIFIER ('<:' baseClass)?
    (':' (interfaceClass)+)?
    annotationBlock?;
baseClass: IDENTIFIER;
interfaceClass: IDENTIFIER;

fieldDefinition: 'static'? IDENTIFIER ':' type annotationBlock?;

methodDefinition:
    methodModifiers IDENTIFIER '(' parameter* ')' ':' type
    annotationBlock?
    codeBlock?;
methodModifiers: 'static'? 'native'?;
parameter: IDENTIFIER ':' type annotationBlock?;

annotationBlock: '[' annotationDefinition+ ']';
annotationDefinition: type ('[' annotationParameter+ ']')?;
annotationParameter: IDENTIFIER '{' (LITERAL+ | STRING_LITERAL+ | IDENTIFIER+ | annotationDefinition+) '}';

// CODE BLOCKS AND STATEMENTS
codeBlock: '{' statement* '}';

statement:
    variableDefinitionStatement | assignmentStatement | expressionStatement |
    conditionalStatement | loopStatement | breakContinueStatement |
    returnStatement;

variableDefinitionStatement: 'var' IDENTIFIER '=' expression;
assignmentStatement: IDENTIFIER '=' expression;
expressionStatement: expression;

conditionalStatement: 'if' expression codeBlock elseBranch?;
elseBranch: 'else' codeBlock;

loopStatement: 'while' expression codeBlock eachBlock?;
eachBlock: 'each' codeBlock;
breakContinueStatement: 'break' | 'continue';

returnStatement: 'return' expression?;

// EXPRESSIONS
expression: andExpression ('||' andExpression)*;
andExpression: mathExpression ('&&' mathExpression)*;

mathExpression: mathTerm (operator mathTerm)*;
mathTerm: unaryOperator* (LITERAL | objectExpression | '(' expression ')');

unaryOperator: '-' | '!' | '[' type ']';
operator:
    '*' | '/' | '%' |
    '+' | '-' |
    '<<' | '>>' | '>>>' |
    '<' | '>' | '<=' | '>=' |
    '==' | '!=' |
    '&' |
    '^' |
    '|';

objectExpression: objectTerm ('.' objectTerm)*;
objectTerm: 'super'? IDENTIFIER ('(' expression* ')')? | STRING_LITERAL;

// TYPES
type: ('void' | 'int' | 'long' | 'float' | 'double' |
    'bool' | 'byte' | 'char' | 'short' | IDENTIFIER) '[]'? '!'?;

// LEXER DEFINITIONS
LITERAL: [0-9]+ ('.' [0-9]+)? ('L' | 'F')?;
STRING_LITERAL: '"' .*? '"';
IDENTIFIER: [a-zA-Z] ([a-zA-Z0-9_$/])*;

LINE_COMMENT: '//' ~[\r\n]* -> skip;
WS: [ \r\n\t]+ -> channel(HIDDEN);
