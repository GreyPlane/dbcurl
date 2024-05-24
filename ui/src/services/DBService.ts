import {atom} from "jotai";
import {atomWithMutation, atomWithSuspenseQuery} from "jotai-tanstack-query"
import axios from "axios";
import {apiRequest} from "../utils/request";
import {atomWithImmer, withImmer} from "jotai-immer";
import {atomWithStorage} from "jotai/utils"

export type DBType = "MySQL" | "PostgrelSQL"

export const DBTypes: DBType[] = ["MySQL", "PostgrelSQL"]

export interface DBProperty {
  username: string,
  password: string,
  url: string,
  dbType: DBType
}

export interface TableDesc {
  name: string
}

export interface SchemaDesc {
  name: string,
  tables: TableDesc[]
}

export interface DBView {
  id: string,
  property: DBProperty,
  schemas: SchemaDesc[]
}

export const dbViewAtom = withImmer(atomWithStorage<DBView[]>("dbViews", []))

export const addDBAtom = atomWithMutation((get) => ({
  mutationKey: ["dbs"],
  mutationFn: async ({id, property}: { id: string, property: DBProperty }) => {
    const response = await apiRequest.post<string>(`db/${id}`, {
      [property.dbType]: {
        ...property
      }
    })
    return response.data
  }
}))

// export const getSchemas = atomWithSuspenseQuery((get) => ({
//
// }))
