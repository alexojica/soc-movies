package soc.movies.integrationtests;


import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import soc.movies.testutils.DbHelpers;
import soc.movies.testutils.HttpHelpers;
import soc.movies.web.Launcher;
import soc.movies.web.dto.MovieCreationRequest;

public class MovieIntegrationTest {

	final Javalin app = Launcher.buildApp();

	@AfterEach
	@BeforeEach
	void cleanUp() {
		DbHelpers.deleteMovie("the-shawshank-redemption");
	}

	@Test
	void createMovie() {
		JavalinTest.test(app, (server, client) -> {
			var response = client.post(
					"/movie",
					shawshankRedemption(),
					HttpHelpers.headers()
			);

			Assertions.assertEquals(201, response.code());
			var body = TypeConversion.toMovieInfoResponse(response);
			Assertions.assertTrue(body.id() > 0);

			Assertions.assertEquals("English", body.language());
			Assertions.assertEquals("The Shawshank Redemption", body.name());
			Assertions.assertEquals("the-shawshank-redemption", body.slug());
			Assertions.assertEquals(List.of("period_drama", "prison_drama", "tragedy", "epic"),
					body.tags());
		});
	}

	@Test
	void createDuplicateMovie() {
		JavalinTest.test(app, (server, client) -> {
			var firstResponse = client.post(
					"/movie",
					shawshankRedemption(),
					HttpHelpers.headers()
			);
			Assertions.assertEquals(201, firstResponse.code());

			var secondResponse = client.post(
					"/movie",
					shawshankRedemption(),
					HttpHelpers.headers()
			);
			Assertions.assertEquals(409, secondResponse.code());
			var body = TypeConversion.toErrorResponse(secondResponse);
			Assertions.assertEquals("Movie already exists", body.message());

		});
	}

	MovieCreationRequest shawshankRedemption() {
		return MovieCreationRequest
				.builder()
				.name("The Shawshank Redemption")
				.description("""
						The Shawshank Redemption is a 1994 American prison drama film written
						and directed by Frank Darabont, based on the 1982 Stephen King novella
						Rita Hayworth and Shawshank Redemption.
						""")
				.tags(List.of("period_drama", "prison_drama", "tragedy", "epic"))
				.language("English")
				.releasedYear(1970)
				.build();
	}
}

