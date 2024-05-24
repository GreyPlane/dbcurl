import React, {Fragment} from "react";
import {MenuItem, TextField} from "@mui/material";
import {atomWithImmer} from "jotai-immer";
import {DBProperty, DBTypes} from "../services/DBService";
import {useAtom} from "jotai";

interface AddDBFormProps {
}

export type AddDBFormData = {
  id: string,
  property: DBProperty
}

export const defaultAddDbRequest: AddDBFormData = {
  id: "localMySQL",
  property: {
    url: "jdbc:mysql://localhost:3306",
    username: "root",
    password: "",
    dbType: "MySQL"
  }
}

export const connectDBFormData = atomWithImmer<AddDBFormData>(defaultAddDbRequest)

export const AddDBForm: React.FC<AddDBFormProps> = (props: AddDBFormProps) => {
  const [formData, setFormData] = useAtom(connectDBFormData);

  return (
    <Fragment>
      <TextField select id="dbType" label="DB Type" value={formData.property.dbType} onChange={e => {
        setFormData(draft => {
          draft.property.dbType = e.target.value as any
        })
      }} margin="dense" fullWidth variant="standard">
        {DBTypes.map(dbType => (
          <MenuItem key={dbType} value={dbType}>
            {dbType}
          </MenuItem>
        ))}
      </TextField>
      <TextField id="id" label="id" value={formData.id} onChange={(e) => {
        setFormData(draft => {
          draft.id = e.target.value
        })
      }} type="text" required margin="dense" fullWidth variant="standard"></TextField>
      <TextField id="username" label="username" value={formData.property.username} onChange={(e) => {
        setFormData(draft => {
          draft.property.username = e.target.value
        })
      }} type="text" required margin="dense" fullWidth variant="standard"></TextField>
      <TextField id="password" label="password" value={formData.property.password} onChange={(e) => {
        setFormData(draft => {
          draft.property.password = e.target.value
        })
      }} type="password" margin="dense" fullWidth variant="standard"></TextField>
      <TextField id="url" label="url" value={formData.property.url} onChange={(e) => {
        setFormData(draft => {
          draft.property.url = e.target.value
        })
      }} type="url" required margin="dense" fullWidth variant="standard"></TextField>
    </Fragment>
  )
}
