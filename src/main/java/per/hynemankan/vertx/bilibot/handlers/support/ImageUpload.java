package per.hynemankan.vertx.bilibot.handlers.support;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.multipart.MultipartForm;
import lombok.extern.slf4j.Slf4j;
import per.hynemankan.vertx.bilibot.expection.BiliApiException;
import per.hynemankan.vertx.bilibot.expection.WebClientException;
import per.hynemankan.vertx.bilibot.utils.CookiesManager;
import per.hynemankan.vertx.bilibot.utils.GlobalConstants;
import per.hynemankan.vertx.bilibot.utils.HeaderAdder;

import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
public class ImageUpload {
  public static Future<JsonObject> puloadImageByPath(WebClient client, String filePath, String fileName) {
    return Future.future(response -> {
      URL url;
      try {
        url = new URL(GlobalConstants.BILI_SUPPORT_IMAGE_UPLOAD_API);
      } catch (MalformedURLException e) {
        response.fail(new WebClientException("Got illegal Url!", e));
        return;
      }
      MultipartForm form = MultipartForm.create();
      form.binaryFileUpload("file_up", fileName, filePath, genMediaType(fileName));
      form.attribute("build", "0");
      form.attribute("mobi_app", "web");
      HttpRequest<Buffer> request = client.post(GlobalConstants.BILI_PORT, url.getHost(), url.getPath());
      HeaderAdder.headerAdd(request);
      CookiesManager.headCookiesAdder(request)
        .onFailure(response::fail).onSuccess(r -> {
        request.sendMultipartForm(form)
          .onFailure(response::fail).onSuccess(httpResponse -> {
          JsonObject body = httpResponse.bodyAsJsonObject();
          if (!body.getInteger("code").equals(0)) {
            log.warn(body.toString());
            response.fail(new BiliApiException(body.toString()));
            return;
          }
          log.info(body.toString());
          response.complete(body);
        });
      });
    });
  }

  private static String genMediaType(String fileName) {
    String fileType = getFileType(fileName);
    return String.format("image/%s", fileType);

  }

  private static String getFileType(String fileName) {
    String[] temp = fileName.split(".");
    return temp[temp.length - 1];
  }
}
