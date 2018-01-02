(ns sinostudy.pinyin.data)

;; also includes special case initials w and y (technically not initials)
(def initials
  #{"b" "p" "m" "f" "d" "t" "n" "l"
    "g" "k" "h" "j" "q" "x" "z" "c"
    "s" "zh" "ch" "sh" "r" "w" "y"})

;; includes all possible forms in use (e.g. "ue" as shorthand for "üe")
(def finals
  #{"a" "ai" "an" "ang" "ao"
    "e" "ei" "en" "eng" "er"
    "i" "ia" "ian" "iang" "iao" "ie" "in" "ing" "iong" "iu"
    "o" "ong" "ou"
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

;; only used to search and replace diacritics
(def diacritic-patterns
  {"a" #"[āáǎà]", "A" #"[ĀÁǍÀ]"
   "o" #"[ōóǒò]", "O" #"[ŌÓǑÒ]"
   "e" #"[ēéěè]", "E" #"[ĒÉĚÈ]"
   "u" #"[ūúǔù]", "U" #"[ŪÚǓÙ]"
   "i" #"[īíǐì]", "I" #"[ĪÍǏÌ]"
   "ü" #"[ǖǘǚǜ]", "Ü" #"[ǕǗǙǛ]"})

;; adapted from http://pinyin.info/rules/initials_finals.html
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

    "fa" "fan" "fang" "fei" "fen" "feng" "fo" "fou" "fu"

    "ga" "gai" "gan" "gang" "gao" "ge" "gei" "gen" "geng" "gong" "gou" "gu"
    "gua" "guai" "guan" "guang" "gui" "gun" "guo"

    "ha" "hai" "han" "hang" "hao" "he" "hei" "hen" "heng" "hong" "hou" "hu"
    "hua" "huai" "huan" "huang" "hui" "hun" "huo"

    "ji" "jia" "jian" "jiang" "jiao" "jie" "jin" "jing" "jiong" "jiu" "ju"
    "juan" "jue" "jun"

    "ka" "kai" "kan" "kang" "kao" "ke" "kei" "ken" "keng" "kong" "kou" "ku"
    "kua" "kuai" "kuan" "kuang" "kui" "kun" "kuo"

    "la" "lai" "lan" "lang" "lao" "le" "lei" "leng" "li" "lia" "lian" "liang"
    "liao" "lie" "lin" "ling" "liu" "long" "lou" "lu" "luan" "lun" "luo" "lü"
    "lüe"

    "ma" "mai" "man" "mang" "mao" "me" "mei" "men" "meng" "mi" "mian" "miao"
    "mie" "min" "ming" "miu" "mo" "mou" "mu"

    "na" "nai" "nan" "nang" "nao" "ne" "nei" "nen" "neng" "ni" "nian" "niang"
    "niao" "nie" "nin" "ning" "niu" "nong" "nou" "nu" "nuan" "nun" "nuo" "nü"
    "nüe"

    "o" "ou"

    "pa" "pai" "pan" "pang" "pao" "pei" "pen" "peng" "pi" "pian" "piao" "pie"
    "pin" "ping" "po" "pou" "pu"

    "qi" "qia" "qian" "qiang" "qiao" "qie" "qin" "qing" "qiong" "qiu" "qu"
    "quan" "que" "qun"

    "ran" "rang" "rao" "re" "ren" "reng" "ri" "rong" "rou" "ru" "rua" "ruan"
    "rui" "run" "ruo"

    "sa" "sai" "san" "sang" "sao" "se" "sen" "seng" "sha" "shai" "shan" "shang"
    "shao" "she" "shei" "shen" "sheng" "shi" "shou" "shu" "shua" "shuai" "shuan"
    "shuang" "shui" "shun" "shuo" "si" "song" "sou" "su" "suan" "sui" "sun"
    "suo"

    "ta" "tai" "tan" "tang" "tao" "te" "tei" "teng" "ti" "tian" "tiao" "tie"
    "ting" "tong" "tou" "tu" "tuan" "tui" "tun" "tuo"

    "wa" "wai" "wan" "wang" "wei" "wen" "weng" "wo" "wu"

    "xi" "xia" "xian" "xiang" "xiao" "xie" "xin" "xing" "xiong" "xiu" "xu"
    "xuan" "xue"

    "ya" "yan" "yang" "yao" "ye" "yi" "yin" "ying" "yong" "you" "yu" "yuan"
    "yue"

    "za" "zai" "zan" "zang" "zao" "ze" "zei" "zen" "zeng" "zha" "zhai" "zhan"
    "zhang" "zhao" "zhe" "zhei" "zhen" "zheng" "zhi" "zhong" "zhou" "zhu" "zhua"
    "zhuai" "zhuan" "zhuang" "zhui" "zhun" "zhuo" "zi" "zong" "zou" "zu" "zuan"
    "zui" "zun" "zuo"})
