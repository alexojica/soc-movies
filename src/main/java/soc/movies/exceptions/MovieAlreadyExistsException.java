package soc.movies.exceptions;

import io.javalin.http.HttpStatus;
import soc.movies.web.dto.ErrorResponse;

public class MovieAlreadyExistsException extends MovieException {

	@Override
	public ErrorResponse buildResponse() {
		return ErrorResponse.build("Movie already exists");
	}

	@Override
	public HttpStatus getStatus() {
		return HttpStatus.CONFLICT;
	}
}
