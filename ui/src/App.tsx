import React from "react";
import {RouterProvider} from "react-router-dom";
import {QueryClient, QueryClientProvider} from "@tanstack/react-query";
import {router} from "./router";
import {
  AppBar,
  Box,
  Button,
  Container,
  createTheme,
  CssBaseline, IconButton,
  ThemeProvider,
  Toolbar,
  Typography
} from "@mui/material";
import {AddDBButton} from "./pages/AddDBButton";

const queryClient = new QueryClient();

export const App: React.FC = () => (
  <QueryClientProvider client={queryClient}>
    <ThemeProvider theme={createTheme()}>
      <Box sx={{display: "flex", flexDirection: "column"}}>
        <CssBaseline></CssBaseline>
        <Box sx={{flexGrow: 1}}>
          <AppBar position="static">
            <Toolbar>
              <Typography variant="h6" component="div" sx={{flexGrow: 1}}>
                dbcurl
              </Typography>
              <AddDBButton></AddDBButton>
            </Toolbar>
          </AppBar>
        </Box>
        <Box sx={{display: "flex", flexDirection: "row"}}>
          <Container component="div">Schemas</Container>
          <Container component="div" sx={{flexGrow: 2}}>Main</Container>
        </Box>
        {/*<Container component="div" sx={{flexGrow: 1, height: "100%"}}>*/}
        {/*  <RouterProvider router={router}></RouterProvider>*/}
        {/*</Container>*/}
      </Box>
    </ThemeProvider>
  </QueryClientProvider>
)

