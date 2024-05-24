import {useAtom} from "jotai";
import {addDBAtom} from "../services/DBService";
import React, {useState} from "react";
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  Modal
} from "@mui/material";
import {
  Add
} from "@mui/icons-material"
import {AddDBForm, connectDBFormData, defaultAddDbRequest} from "../components/AddDBForm";
import {LoadingButton} from "@mui/lab";

interface AddDBButtonProps {

}

export const AddDBButton: React.FC<AddDBButtonProps> = ({}) => {
  const [showForm, setShowForm] = useState<boolean>(false)
  const [request, setRequest] = useAtom(connectDBFormData);

  const [{mutateAsync, isPending, mutate}] = useAtom(addDBAtom)


  return (
    <>
      <IconButton color="inherit" edge="end" aria-label="add-db" onClick={() => setShowForm(true)}>
        <Add></Add>
      </IconButton>

      <Dialog open={showForm} PaperProps={{
        component: "form"
      }}>
        <DialogTitle>Add DB</DialogTitle>
        <DialogContent>
          <DialogContentText>Properties for connecting database</DialogContentText>
          <AddDBForm></AddDBForm>
        </DialogContent>
        <DialogActions>
          <LoadingButton variant="contained"
                         loading={isPending}
                         loadingPosition="start"
                         onClick={() => {
                           mutate(request, {
                             onSuccess: (data) => {

                             }
                           })
                         }}>Confirm</LoadingButton>

          <Button onClick={() => {
            setShowForm(false)
            setRequest(defaultAddDbRequest)
          }}>Cancel</Button>
        </DialogActions>
      </Dialog>
    </>
  )
}
