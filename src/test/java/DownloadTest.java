import downloaders.Download;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import util.Constants;

public class DownloadTest {
    public static Download dl;
    @BeforeAll
    static void init() {
        dl = new Download("");
    }
    @Test
    void isValidUrlTest(){
        dl.setUrl("");
        assert(!dl.isValidUrl());
        dl.setUrl(Constants.ytPrefix);
        assert(!dl.isValidUrl());
        dl.setUrl(Constants.ytPrefix + Constants.vidPrefix);
        assert(dl.isValidUrl());
        dl.setUrl(Constants.testURL);
        assert(dl.isValidUrl());
    }
}
