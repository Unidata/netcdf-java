module Jekyll
  class IncludeCodeBlockTag < Liquid::Tag
    def initialize(tag_name, text, tokens)
      super
      arr = text.split("&")
      if arr.length < 3
        raise "Syntax error in includecodeblock: " + text
      else
        @projdir = arr[0].strip
        @path = arr[1].strip
        @function = arr[2].strip
        ext = /\.[a-zA-Z]+/ =~ @path
        @lang = @path[ext+1..-1]
      end
    end
    def render(context)
      # read file
      projdir = @projdir
      dirIndex = /#{projdir}\// =~ Dir.pwd
      projpath = Dir.pwd[0..dirIndex + projdir.length]
      filetext = File.read File.join projpath, @path

      # init function text
      codetext = ""

      # find function start
      startscan = /#{@function}\(.*\)\s*\{/ =~ filetext

      # find function end
      unless startscan.nil?
        start = filetext.index("{", startscan)
        i = start
        brackets = ["{"]
        while brackets.length > 0
          i+=1
          c = filetext[i]
          if c == "{"
            brackets.push(c)
          elsif c == "}"
            brackets.pop()
          end
        end
        codetext = format(filetext[start..i])
      end

      # include the text annotated with code syntax
      "~~~" + @lang + "\n" + codetext + "\n~~~"
    end

    ##
    # left align code block
    def format(codestring)
      lines = codestring.split("\n")
      # remove first and last lines (function declaration and closing bracket)
      codelines = lines[1..-2]
      # remove fixed num of tab characters from each line
      offset = /\S/ =~ codelines[0]
      codelines.map! { |s| s[offset..-1]}
      # rejoin and return
      return  codelines.join("\n")
    end
  end
end
Liquid::Template.register_tag('includecodeblock', Jekyll::IncludeCodeBlockTag)
