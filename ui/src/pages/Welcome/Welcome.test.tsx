import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { Welcome } from "./Welcome";

test("renders text content", () => {
  render(
    <MemoryRouter initialEntries={["/login"]}>
      <Welcome />
    </MemoryRouter>,
  );
  const header = screen.getByText("Welcome to Dbcrul!");
  expect(header).toBeInTheDocument();
});
