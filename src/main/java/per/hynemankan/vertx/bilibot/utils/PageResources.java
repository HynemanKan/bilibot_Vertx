package per.hynemankan.vertx.bilibot.utils;

public class PageResources {
  public static final String MAIN_PAGE="<!DOCTYPE html>\n" +
    "<html lang=\"zh-CN\">\n" +
    "<head>\n" +
    "  <meta charset=\"utf-8\">\n" +
    "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
    "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
    "  <!-- 上述3个meta标签*必须*放在最前面，任何其他内容都*必须*跟随其后！ -->\n" +
    "  <meta name=\"description\" content=\"\">\n" +
    "  <meta name=\"author\" content=\"\">\n" +
    "  <link rel=\"icon\" href=\"static/img/favicon.ico\">\n" +
    "  <title>登录</title>\n" +
    "  <script src=\"http://code.jquery.com/jquery-2.1.1.min.js\"></script>\n" +
    "  <script src=\"https://cdn.bootcdn.net/ajax/libs/twitter-bootstrap/4.5.2/js/bootstrap.min.js\"></script>\n" +
    "  <script src=\"https://cdn.bootcdn.net/ajax/libs/qrcodejs/1.0.0/qrcode.min.js\"></script>\n" +
    "  <!-- Bootstrap core CSS -->\n" +
    "  <link href=\"https://cdn.bootcdn.net/ajax/libs/twitter-bootstrap/4.5.2/css/bootstrap.min.css\" rel=\"stylesheet\">\n" +
    "\n" +
    "  <style>\n" +
    "            body {\n" +
    "              padding-top: 20px;\n" +
    "              padding-bottom: 20px;\n" +
    "            }\n" +
    "            @media (min-width: 768px) {\n" +
    "              .container {\n" +
    "                max-width: 730px;\n" +
    "              }\n" +
    "            }\n" +
    "            .container-narrow > hr {\n" +
    "              margin: 30px 0;\n" +
    "            }\n" +
    "\n" +
    "        </style>\n" +
    "  <![endif]-->\n" +
    "</head>\n" +
    "<body style=\"background-color:#FAFFF0;color: #292421;\">\n" +
    "<div class=\"container\">\n" +
    "  <div class=\"row\">\n" +
    "    <div class=\"col-lg-4 col-md-4 col-sm-4 col-xs-4 col-lg-push-4 col-md-push-4 col-sm-push-4 col-xs-push-4\">\n" +
    "      <h2>请扫描登录</h2>\n" +
    "      <div id=\"qrcode\" style=\"height: 256px;width: 256px;\"></div>\n" +
    "      <p>剩余时间<span id=\"time\"></span>s</p>\n" +
    "      <p>登录状态:<span id=\"loginState\"></span></p>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>\n" +
    "\n" +
    "<script>\n" +
    "  var lifeTime = 180;\n" +
    "  function loop(){\n" +
    "      lifeTime -=1\n" +
    "      if (lifeTime<=0){\n" +
    "          location.reload();\n" +
    "      }\n" +
    "      $(\"#time\").text(lifeTime);\n" +
    "      $.getJSON(\"/API/login/getLoginStatus\",function (jsonData) {\n" +
    "        console.log(jsonData);\n" +
    "        if (jsonData.code==\"\"){\n" +
    "          loginStatus=jsonData.data.loginStatus;\n" +
    "          if (loginStatus == \"OAUTH_TOKEN_UNSCAN\"){\n" +
    "            $(\"#loginState\").text(\"待扫码\");\n" +
    "          }else if(isScan == \"OAUTH_TOKEN_UNCOMFIRMED\"){\n" +
    "            $(\"#loginState\").text(\"未确认\");\n" +
    "          }else if(isScan ==\"OAUTH_SUCCESS\"){\n" +
    "            window.location.href='/state';\n" +
    "          }else{\n" +
    "            location.reload();\n" +
    "          }\n" +
    "        }\n" +
    "      })\n" +
    "  }\n" +
    "\n" +
    "\n" +
    "  $.getJSON(\"/API/login/getQRCode\",function(jsonData){\n" +
    "      console.log(jsonData);\n" +
    "      if(jsonData.code==\"1\"){\n" +
    "        window.location.href='/state';\n" +
    "      }\n" +
    "      new QRCode(document.getElementById(\"qrcode\"), {\n" +
    "          text:jsonData.data.url,\n" +
    "          width: 256,\n" +
    "          height: 256,\n" +
    "          colorDark : \"#292421\",\n" +
    "          colorLight : \"#FAFFF0\",\n" +
    "      });\n" +
    "      setInterval(loop,1000)\n" +
    "      })\n" +
    "</script>\n" +
    "</body>\n" +
    "</html>\n";
}
