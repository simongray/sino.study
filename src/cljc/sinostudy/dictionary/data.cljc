(ns sinostudy.dictionary.data)

(def stopwords
  #{"a" "about" "above" "across" "after" "afterwards" "again" "against"
    "all" "almost" "alone" "along" "already" "also" "although" "always" "am"
    "among" "amongst" "amount"  "an" "and" "another" "any" "anyhow"
    "anyone" "anything" "anyway" "anywhere" "are" "around" "as"  "at" "back"
    "be" "became" "because" "become" "becomes" "becoming" "been" "before"
    "beforehand" "behind" "being" "below" "beside" "besides" "between"
    "beyond" "bill" "both" "bottom" "but" "by" "call" "can" "cannot" "cant" "co"
    "con" "could" "couldnt" "cry" "de" "describe" "detail" "do" "does" "done"
    "down" "due" "during" "each" "eg" "eight" "either" "eleven" "else"
    "elsewhere" "empty" "enough" "etc" "even" "ever" "every" "everyone"
    "everything" "everywhere" "except" "few" "fifteen" "fify" "fill" "find"
    "fire" "first" "five" "for" "former" "formerly" "forty" "found" "four"
    "from" "front" "full" "further" "get" "give" "go" "had" "has" "hasnt" "have"
    "he" "hence" "her" "here" "hereafter" "hereby" "herein" "hereupon" "hers"
    "herself" "him" "himself" "his" "how" "however" "hundred" "i" "ie" "if" "in"
    "inc" "indeed" "interest" "into" "is" "it" "its" "itself" "keep" "last"
    "latter" "latterly" "least" "less" "ltd" "made" "many" "may" "me"
    "meanwhile" "might" "mill" "mine" "more" "moreover" "most" "mostly" "move"
    "much" "must" "my" "myself" "name" "namely" "neither" "never" "nevertheless"
    "next" "nine" "no" "nobody" "none" "noone" "nor" "not" "nothing" "now"
    "nowhere" "of" "off" "often" "on" "once" "one" "only" "onto" "or" "other"
    "others" "otherwise" "our" "ours" "ourselves" "out" "over" "own" "part"
    "per" "perhaps" "please" "put" "rather" "re" "same" "see" "seem" "seemed"
    "seeming" "seems" "serious" "several" "she" "should" "show" "side" "since"
    "sincere" "six" "sixty" "so" "some" "somehow" "someone" "something"
    "sometime" "sometimes" "somewhere" "still" "such" "system" "take" "ten"
    "than" "that" "the" "their" "them" "themselves" "then" "thence" "there"
    "thereafter" "thereby" "therefore" "therein" "thereupon" "these" "they"
    "thick" "thin" "third" "this" "those" "though" "three" "through"
    "throughout" "thru" "thus" "to" "together" "too" "top" "toward" "towards"
    "twelve" "twenty" "two" "un" "under" "until" "up" "upon" "us" "very" "via"
    "was" "we" "well" "were" "what" "whatever" "when" "whence" "whenever"
    "where" "whereafter" "whereas" "whereby" "wherein" "whereupon" "wherever"
    "whether" "which" "while" "whither" "who" "whoever" "whole" "whom" "whose"
    "why" "will" "with" "within" "without" "would" "yet" "you" "your" "yours"
    "yourself" "yourselves"

    ;; Common contractions with apostrophe (and a few without)
    ;; https://en.wikipedia.org/wiki/Wikipedia:List_of_English_contractions
    "ain't"
    "aren't"
    "can't"
    "could've"
    "couldn't"
    "daren't"
    "daresn't"
    "dasn't"
    "didn't"
    "doesn't"
    "don't"
    "e'er"
    "everyone's"
    "finna"
    "gimme"
    "gonna"
    "gotta"
    "hadn't"
    "hasn't"
    "haven't"
    "he'd"
    "he'll"
    "he's"
    "he've"
    "how'd"
    "how'll"
    "how're"
    "how's"
    "I'd"
    "I'll"
    "I'm"
    "I'm'a"
    "I'm'o"
    "I've"
    "isn't"
    "it'd"
    "it'll"
    "it's"
    "let's"
    "ma'am"
    "mayn't"
    "may've"
    "mightn't"
    "might've"
    "mustn't"
    "mustn't've"
    "must've"
    "needn't"
    "ne'er"
    "o'clock"
    "o'er"
    "ol'"
    "oughtn't"
    "'s"
    "shan't"
    "she'd"
    "she'll"
    "she's"
    "should've"
    "shouldn't"
    "somebody's"
    "someone's"
    "something's"
    "that'll"
    "that're"
    "that's"
    "that'd"
    "there'd"
    "there'll"
    "there're"
    "there's"
    "these're"
    "they'd"
    "they'll"
    "they're"
    "they've"
    "this's"
    "those're"
    "'tis"
    "'twas"
    "wasn't"
    "we'd"
    "we'd've"
    "we'll"
    "we're"
    "we've"
    "weren't"
    "what'd"
    "what'll"
    "what're"
    "what's"
    "what've"
    "when's"
    "where'd"
    "where're"
    "where's"
    "where've"
    "which's"
    "who'd"
    "who'd've"
    "who'll"
    "who're"
    "who's"
    "who've"
    "why'd"
    "why're"
    "why's"
    "won't"
    "would've"
    "wouldn't"
    "y'all"
    "y'all'd've"
    "yesn't"
    "you'd"
    "you'll"
    "you're"
    "you've"
    "noun's"
    "noun(s)'re"

    ;; Special cases (common throughout CC-CEDICT definitions)
    "variant" "loanword" "cf" "lit" "tw" "pr" "abbr" "taiwan" "radical" "kangxi"
    "arch" "archaic" "...er" "written"

    ;; Numbers are excluded
    "1"
    "2"
    "3"
    "4"
    "5"
    "6"
    "7"
    "8"
    "9"
    "0"

    ;; The entire English alphabet is excluded ("a" and "i" found above)
    ;"a"
    "b"
    "c"
    "d"
    "e"
    "f"
    "g"
    "h"
    ;"i"
    "j"
    "k"
    "l"
    "m"
    "n"
    "o"
    "p"
    "q"
    "r"
    "s"
    "t"
    "u"
    "v"
    "w"
    "x"
    "y"
    "z"})
