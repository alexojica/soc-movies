package soc.movies.web.controller;

import static soc.movies.common.Constants.AUTH_HEADER_NAME;
import static soc.movies.common.Elasticsearch.getESClient;

import co.elastic.clients.elasticsearch.core.search.Hit;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import lombok.SneakyThrows;
import org.jooq.SQLDialect;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.jooq.impl.DSL;
import soc.movies.common.Environment;
import soc.movies.common.TextTransformer;
import soc.movies.entities.MovieEntity;
import soc.movies.entities.RatingEntity;
import soc.movies.entities.UserEntity;
import soc.movies.exceptions.InvalidRatingException;
import soc.movies.exceptions.MovieAlreadyExistsException;
import soc.movies.exceptions.MovieNotFoundException;
import soc.movies.exceptions.RatingAlreadyExistsException;
import soc.movies.exceptions.UnauthenticatedRequest;
import soc.movies.exceptions.UserNotFoundException;
import soc.movies.services.MovieService;
import soc.movies.web.dto.ErrorResponse;
import soc.movies.web.dto.MovieCreationRequest;
import soc.movies.web.dto.MovieInfoResponse;
import soc.movies.web.dto.MovieSearchResponse;
import soc.movies.web.dto.RateMovieRequest;
import soc.movies.web.dto.UserInfoResponse;

public class MovieController {

	private final MovieService movieService;

	public MovieController() {
		this.movieService = new MovieService();
	}

	@SneakyThrows
	@OpenApi(
			summary = "Create movie",
			operationId = "createMovie",
			path = "/movie",
			requestBody = @OpenApiRequestBody(required = true, content = {
					@OpenApiContent(from = MovieCreationRequest.class)}),
			methods = HttpMethod.POST,
			headers = {
					@OpenApiParam(name = AUTH_HEADER_NAME, required = true, description = "Authentication Token")},
			responses = {
					@OpenApiResponse(status = "201", content = {
							@OpenApiContent(from = UserInfoResponse.class)}),
					@OpenApiResponse(status = "400", content = {
							@OpenApiContent(from = ErrorResponse.class)}),
					@OpenApiResponse(status = "401", content = {
							@OpenApiContent(from = ErrorResponse.class)})
			}
	)
	public void createMovie(Context ctx) {
		if (!Environment.getApiSecret().equals(ctx.header(AUTH_HEADER_NAME))) {
			throw new UnauthenticatedRequest();
		}
		var request = ctx.bodyAsClass(MovieCreationRequest.class);
		var movie = movieService.createMovie(request);
		ctx.json(MovieInfoResponse.build(movie));
		ctx.status(HttpStatus.CREATED);
	}

	@SneakyThrows
	@OpenApi(
			summary = "Fetch movie",
			operationId = "retrieveMovie",
			path = "/movie/{slug}",
			methods = HttpMethod.GET,
			pathParams = {
					@OpenApiParam(name = "slug", type = String.class, description = "Movie identifier")
			},
			headers = {
					@OpenApiParam(name = AUTH_HEADER_NAME, required = true, description = "Authentication Token")},
			responses = {
					@OpenApiResponse(status = "200", content = {
							@OpenApiContent(from = UserInfoResponse.class)}),
					@OpenApiResponse(status = "400", content = {
							@OpenApiContent(from = ErrorResponse.class)}),
					@OpenApiResponse(status = "401", content = {
							@OpenApiContent(from = ErrorResponse.class)})
			}
	)
	public void retrieveMovie(Context ctx) {
		String slug = ctx.pathParam("slug");
		MovieEntity movie = movieService.fetchMovie(slug);

			if (!Environment.getApiSecret().equals(ctx.header(AUTH_HEADER_NAME))) {
				throw new UnauthenticatedRequest();
			}

			ctx.json(MovieInfoResponse.build(movie));
			ctx.status(HttpStatus.OK);
	}

	@SneakyThrows
	@OpenApi(
			summary = "Rate movie",
			operationId = "rateMovie",
			path = "/movie/{slug}/rate",
			methods = HttpMethod.POST,
			requestBody = @OpenApiRequestBody(required = true, content = {
					@OpenApiContent(from = RateMovieRequest.class)}),
			pathParams = {
					@OpenApiParam(name = "slug", type = String.class, description = "Movie identifier")
			},
			headers = {
					@OpenApiParam(name = AUTH_HEADER_NAME, required = true, description = "Authentication Token")},
			responses = {
					@OpenApiResponse(status = "200", content = {
							@OpenApiContent(from = UserInfoResponse.class)}),
					@OpenApiResponse(status = "400", content = {
							@OpenApiContent(from = ErrorResponse.class)}),
					@OpenApiResponse(status = "401", content = {
							@OpenApiContent(from = ErrorResponse.class)})
			}
	)
	public void rateMovie(Context ctx) {
		String slug = ctx.pathParam("slug");
		var request = ctx.bodyAsClass(RateMovieRequest.class);

		if (request.rating() > 10 || request.rating() < 0) {
			throw new InvalidRatingException();
		}

		if (!Environment.getApiSecret().equals(ctx.header(AUTH_HEADER_NAME))) {
			throw new UnauthenticatedRequest();
		}

		movieService.rateMovie(request.username(), slug, request.rating());
		var movie = movieService.fetchMovie(slug);

		ctx.json(MovieInfoResponse.build(movie));
		ctx.status(HttpStatus.OK);
	}

	@SneakyThrows
	@OpenApi(
			summary = "Search movie",
			operationId = "retrieveMovie",
			path = "/search/movie?q={query}",
			methods = HttpMethod.GET,
			queryParams = {
					@OpenApiParam(name = "q", type = String.class, description = "Search text")
			},
			headers = {
					@OpenApiParam(name = AUTH_HEADER_NAME, required = true, description = "Authentication Token")},
			responses = {
					@OpenApiResponse(status = "200", content = {
							@OpenApiContent(from = UserInfoResponse.class)}),
					@OpenApiResponse(status = "400", content = {
							@OpenApiContent(from = ErrorResponse.class)}),
					@OpenApiResponse(status = "401", content = {
							@OpenApiContent(from = ErrorResponse.class)})
			}
	)
	public void searchMovie(Context ctx) {
		String qWords = ctx.queryParam("q");

		var hits = getESClient()
				.search(s -> s
								.index(Environment.getEsIndex())
								.query(q -> q.simpleQueryString(t -> t.query(qWords))),
						MovieEntity.class)
				.hits()
				.hits();

		var movies = hits
				.stream()
				.sorted((o1, o2) -> o2.score().compareTo(o1.score()))
				.map(Hit::source)
				.toList();

		if (!Environment.getApiSecret().equals(ctx.header(AUTH_HEADER_NAME))) {
			throw new UnauthenticatedRequest();
		}

		ctx.status(HttpStatus.OK);
		ctx.json(MovieSearchResponse.build(qWords, movies));
	}
}
