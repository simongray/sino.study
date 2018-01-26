(ns sinostudy.pinyin.data)

;; also includes special case initials w and y (technically not initials)
(def initials
  #{"b" "p" "m" "f" "d" "t" "n" "l"
    "g" "k" "h" "j" "q" "x" "z" "c"
    "s" "zh" "ch" "sh" "r" "w" "y"})

;; includes all possible forms in use (e.g. "ue" as shorthand for "üe")
;; r is a common special case final (technically not a final)
;; m is a super rare, special case final
(def finals
  #{"a" "ai" "an" "ang" "ao"
    "e" "ei" "en" "eng" "er"
    "i" "ia" "ian" "iang" "iao" "ie" "in" "ing" "iong" "iu"
    "m"
    "o" "ong" "ou"
    "r"
    "u" "ua" "uai" "uan" "uang" "ue" "ui" "un" "uo"
    "ü" "üe"})

;; the index of a character correspond to the tone present at that index
;; indexes 0 and 5 both represent neutral tone (= no diacritics)
(def diacritics
  {\a "aāáǎàa", \A "AĀÁǍÀA"
   \o "oōóǒòo", \O "OŌÓǑÒO"
   \e "eēéěèe", \E "EĒÉĚÈE"
   \u "uūúǔùu", \U "UŪÚǓÙU"
   \i "iīíǐìi", \I "IĪÍǏÌI"
   \ü "üǖǘǚǜü", \Ü "ÜǕǗǙǛÜ"})

;; m is a super rare, special case final
;; the vec is index-aligned like the diacritics above (skip 6 for upper case)
;; note: the diacriticised versions are multi-char and may ruin formatting!
(def m-diacritics
  ["m" "m̄" "ḿ" "m̌" "m̀" "m"
   "M" "M̄" "Ḿ" "M̌" "M̀" "M"])

;; only used to search and replace diacritics
;; also handles special case diacritic char, m
(def diacritic-patterns
  {"a" #"[āáǎà]", "A" #"[ĀÁǍÀ]"
   "o" #"[ōóǒò]", "O" #"[ŌÓǑÒ]"
   "e" #"[ēéěè]", "E" #"[ĒÉĚÈ]"
   "u" #"[ūúǔù]", "U" #"[ŪÚǓÙ]"
   "i" #"[īíǐì]", "I" #"[ĪÍǏÌ]"
   "ü" #"[ǖǘǚǜ]", "Ü" #"[ǕǗǙǛ]"
   "m" #"(m̄|ḿ|m̌|m̀)" "M" #"(M̄|Ḿ|M̌|M̀)"})

;; used to match diacritics to tones in diacritics->digits
(def tone-diacritics
  {1 #"(ā|ō|ē|ū|ī|ǖ|m̄|Ā|Ō|Ē|Ū|Ī|Ǖ|M̄)"
   2 #"(á|ó|é|ú|í|ǘ|ḿ|Á|Ó|É|Ú|Í|Ǘ|Ḿ)"
   3 #"(ǎ|ǒ|ě|ǔ|ǐ|ǚ|m̌|Ǎ|Ǒ|Ě|Ǔ|Ǐ|Ǚ|M̌)"
   4 #"(à|ò|è|ù|ì|ǜ|m̀|À|Ò|È|Ù|Ì|Ǜ|M̀)"})

;; adapted from http://pinyin.info/rules/initials_finals.html
;; some non-standard syllables have been added: fiao, lo, r, yo
(def syllables
  #{"a" "ai" "an" "ang" "ao"

    "ba" "bai" "ban" "bang" "bao" "bei" "ben" "beng" "bi" "bian" "biao" "bie"
    "bin" "bing" "bo" "bu"

    "ca" "cai" "can" "cang" "cao" "ce" "cen" "ceng" "cha" "chai" "chan" "chang"
    "chao" "che" "chen" "cheng" "chi" "chong" "chou" "chu" "chua" "chuai"
    "chuan" "chuang" "chui" "chun" "chuo" "ci" "cong" "cou" "cu" "cuan" "cui"
    "cun" "cuo"

    "da" "dai" "dan" "dang" "dao" "de" "dei" "den" "deng" "di" "dia" "dian"
    "diao" "die" "ding" "diu" "dong" "dou" "du" "duan" "dui" "dun" "duo"

    "e" "ei" "en" "eng" "er"

    "fa" "fan" "fang" "fei" "fen" "feng" "fiao" "fo" "fou" "fu"

    "ga" "gai" "gan" "gang" "gao" "ge" "gei" "gen" "geng" "gong" "gou" "gu"
    "gua" "guai" "guan" "guang" "gui" "gun" "guo"

    "ha" "hai" "han" "hang" "hao" "he" "hei" "hen" "heng" "hong" "hou" "hu"
    "hua" "huai" "huan" "huang" "hui" "hun" "huo"

    "ji" "jia" "jian" "jiang" "jiao" "jie" "jin" "jing" "jiong" "jiu" "ju"
    "juan" "jue" "jun"

    "ka" "kai" "kan" "kang" "kao" "ke" "kei" "ken" "keng" "kong" "kou" "ku"
    "kua" "kuai" "kuan" "kuang" "kui" "kun" "kuo"

    "la" "lai" "lan" "lang" "lao" "le" "lei" "leng" "li" "lia" "lian" "liang"
    "liao" "lie" "lin" "ling" "liu" "lo" "long" "lou" "lu" "luan" "lun" "luo"
    "lü" "lüe"

    "m" "ma" "mai" "man" "mang" "mao" "me" "mei" "men" "meng" "mi" "mian" "miao"
    "mie" "min" "ming" "miu" "mo" "mou" "mu"

    "na" "nai" "nan" "nang" "nao" "ne" "nei" "nen" "neng" "ni" "nian" "niang"
    "niao" "nie" "nin" "ning" "niu" "nong" "nou" "nu" "nuan" "nun" "nuo" "nü"
    "nüe"

    "o" "ou"

    "pa" "pai" "pan" "pang" "pao" "pei" "pen" "peng" "pi" "pian" "piao" "pie"
    "pin" "ping" "po" "pou" "pu"

    "qi" "qia" "qian" "qiang" "qiao" "qie" "qin" "qing" "qiong" "qiu" "qu"
    "quan" "que" "qun"

    "r" "ran" "rang" "rao" "re" "ren" "reng" "ri" "rong" "rou" "ru" "rua" "ruan"
    "rui" "run" "ruo"

    "sa" "sai" "san" "sang" "sao" "se" "sen" "seng" "sha" "shai" "shan" "shang"
    "shao" "she" "shei" "shen" "sheng" "shi" "shou" "shu" "shua" "shuai" "shuan"
    "shuang" "shui" "shun" "shuo" "si" "song" "sou" "su" "suan" "sui" "sun"
    "suo"

    "ta" "tai" "tan" "tang" "tao" "te" "tei" "teng" "ti" "tian" "tiao" "tie"
    "ting" "tong" "tou" "tu" "tuan" "tui" "tun" "tuo"

    "wa" "wai" "wan" "wang" "wei" "wen" "weng" "wo" "wu"

    "xi" "xia" "xian" "xiang" "xiao" "xie" "xin" "xing" "xiong" "xiu" "xu"
    "xuan" "xun" "xue"

    "ya" "yan" "yang" "yao" "ye" "yi" "yin" "ying" "yo" "yong" "you" "yu" "yuan"
    "yun" "yue"

    "za" "zai" "zan" "zang" "zao" "ze" "zei" "zen" "zeng" "zha" "zhai" "zhan"
    "zhang" "zhao" "zhe" "zhei" "zhen" "zheng" "zhi" "zhong" "zhou" "zhu" "zhua"
    "zhuai" "zhuan" "zhuang" "zhui" "zhun" "zhuo" "zi" "zong" "zou" "zu" "zuan"
    "zui" "zun" "zuo"})

;; from http://kourge.net/projects/regexp-unicode-block
(def hanzi-unicode
  {"CJK Radicals Supplement"            #"\u2E80-\u2EFF"
   "Kangxi Radicals"                    #"\u2F00-\u2FDF"
   "Ideographic Description Characters" #"\u2FF0-\u2FFF"
   "CJK Symbols and Punctuation"        #"\u3000-\u303F"
   "CJK Strokes"                        #"\u31C0-\u31EF"
   "Enclosed CJK Letters and Months"    #"\u3200-\u32FF"
   "CJK Compatibility"                  #"\u3300-\u33FF"
   "CJK Unified Ideographs Extension A" #"\u3400-\u4DBF"
   "Yijing Hexagram Symbols"            #"\u4DC0-\u4DFF"
   "CJK Unified Ideographs"             #"\u4E00-\u9FFF"
   "CJK Compatibility Ideographs"       #"\uF900-\uFAFF"
   "CJK Compatibility Forms"            #"\uFE30-\uFE4F"})
