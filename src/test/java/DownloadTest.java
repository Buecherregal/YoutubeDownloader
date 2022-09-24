import downloaders.Download;
import org.junit.jupiter.api.Test;
import util.Constants;

public class DownloadTest {
    @Test
    void isValidUrlTest(){
        Download dl = new Download("");
        assert(!dl.isValidUrl());
        dl = new Download(Constants.ytPrefix);
        assert(!dl.isValidUrl());
        dl = new Download(Constants.ytPrefix + Constants.vidPrefix);
        assert(dl.isValidUrl());
        dl = new Download(Constants.testURL);
        assert(dl.isValidUrl());

    }
}
